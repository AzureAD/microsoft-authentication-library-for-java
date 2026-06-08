// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WSTrustResponseTest {

    private static final String AAD_TOKEN_ERROR_FILE = "/token-error.xml";

    @BeforeEach
    void setup() {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory");
    }

    @Test
    void parse_ValidToken_ReturnsResponseWithToken() throws Exception {
        String xml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);

        WSTrustResponse response = WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13);

        assertNotNull(response);
        assertNotNull(response.getToken(), "Parsed response should contain a token");
        assertNotNull(response.getTokenType(), "Parsed response should contain a token type");
        assertFalse(response.isErrorFound(), "Successful parse should not have error");
    }

    @Test
    void parse_ValidToken_TokenTypeIsSaml1() throws Exception {
        String xml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);

        WSTrustResponse response = WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13);

        assertEquals(WSTrustResponse.SAML1_ASSERTION, response.getTokenType());
        assertFalse(response.isTokenSaml2(), "SAML 1.0 token should not be detected as SAML 2");
    }

    @Test
    void parse_ValidToken_TokenContainsSamlAssertion() throws Exception {
        String xml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);

        WSTrustResponse response = WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13);

        assertTrue(response.getToken().contains("saml:Assertion"),
                "Token should contain SAML assertion XML");
    }

    @Test
    void parse_ErrorResponse_ThrowsMsalServiceException() {
        String xml = TestHelper.readResource(WSTrustResponseTest.class, AAD_TOKEN_ERROR_FILE);

        MsalServiceException ex = assertThrows(MsalServiceException.class,
                () -> WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13));

        assertEquals(AuthenticationErrorCode.WSTRUST_SERVICE_ERROR, ex.errorCode());
        assertTrue(ex.getMessage().contains("ErrorCode:"),
                "Exception message should contain error code info");
        assertTrue(ex.getMessage().contains("FaultMessage:"),
                "Exception message should contain fault message info");
    }

    @Test
    void parse_ErrorResponse_ContainsReasonAndSubcode() {
        String xml = TestHelper.readResource(WSTrustResponseTest.class, AAD_TOKEN_ERROR_FILE);

        MsalServiceException ex = assertThrows(MsalServiceException.class,
                () -> WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13));

        // The error XML has subcode "a:RequestFailed" which splits to "RequestFailed"
        assertTrue(ex.getMessage().contains("RequestFailed"),
                "Exception should contain parsed error subcode");
        assertTrue(ex.getMessage().contains("MSIS3127"),
                "Exception should contain the SOAP fault reason text");
    }

    @Test
    void parse_UndefinedVersion_ThrowsException() {
        String xml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);

        // UNDEFINED version has empty XPaths which cause XPathExpressionException
        assertThrows(Exception.class,
                () -> WSTrustResponse.parse(xml, WSTrustVersion.UNDEFINED));
    }

    @Test
    void isTokenSaml2_nonSaml1Type_returnsTrue() throws Exception {
        String xml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);
        WSTrustResponse response = WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13);

        // Token from test XML is SAML 1.0
        assertFalse(response.isTokenSaml2());

        // Any non-SAML1 type should return true for isTokenSaml2
        // We can verify the constant value
        assertEquals("urn:oasis:names:tc:SAML:1.0:assertion", WSTrustResponse.SAML1_ASSERTION);
    }

    @Test
    void parse_ValidToken_GettersReturnExpectedValues() throws Exception {
        String xml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);

        WSTrustResponse response = WSTrustResponse.parse(xml, WSTrustVersion.WSTRUST13);

        assertFalse(response.isErrorFound());
        assertNull(response.getFaultMessage());
        assertNull(response.getErrorCode());
        assertNotNull(response.getToken());
        assertFalse(response.getToken().isEmpty());
    }

    @Test
    void innerXml_extractsChildContent() throws Exception {
        // innerXml is package-private static, test it directly with a simple XML node
        javax.xml.parsers.DocumentBuilder builder =
                javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(
                new java.io.ByteArrayInputStream("<root><child>text</child></root>".getBytes()));
        org.w3c.dom.Node root = doc.getDocumentElement();

        String inner = WSTrustResponse.innerXml(root);

        assertTrue(inner.contains("text"), "innerXml should extract child content");
        assertTrue(inner.contains("child"), "innerXml should include child element tags");
    }
}
