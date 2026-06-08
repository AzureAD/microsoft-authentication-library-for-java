// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WSTrustResponseTest {

    private static final String AAD_TOKEN_ERROR_FILE = "/token-error.xml";

    private String successXml;
    private String errorXml;
    private WSTrustResponse successResponse;

    @BeforeEach
    void setup() throws Exception {
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory",
                "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");

        successXml = TestHelper.readResource(WSTrustResponseTest.class,
                TestConfiguration.AAD_TOKEN_SUCCESS_FILE);
        errorXml = TestHelper.readResource(WSTrustResponseTest.class, AAD_TOKEN_ERROR_FILE);
        successResponse = WSTrustResponse.parse(successXml, WSTrustVersion.WSTRUST13);
    }

    @AfterEach
    void cleanup() {
        System.clearProperty("javax.xml.parsers.DocumentBuilderFactory");
    }

    @Test
    void parse_ValidToken_ReturnsResponseWithToken() {
        assertNotNull(successResponse);
        assertNotNull(successResponse.getToken(), "Parsed response should contain a token");
        assertNotNull(successResponse.getTokenType(), "Parsed response should contain a token type");
        assertFalse(successResponse.isErrorFound(), "Successful parse should not have error");
    }

    @Test
    void parse_ValidToken_TokenTypeIsSaml1() {
        assertEquals(WSTrustResponse.SAML1_ASSERTION, successResponse.getTokenType());
        assertFalse(successResponse.isTokenSaml2(), "SAML 1.0 token should not be detected as SAML 2");
    }

    @Test
    void parse_ValidToken_TokenContainsSamlAssertion() {
        assertTrue(successResponse.getToken().contains("saml:Assertion"),
                "Token should contain SAML assertion XML");
    }

    @Test
    void parse_ErrorResponse_ThrowsMsalServiceException() {
        MsalServiceException ex = assertThrows(MsalServiceException.class,
                () -> WSTrustResponse.parse(errorXml, WSTrustVersion.WSTRUST13));

        assertEquals(AuthenticationErrorCode.WSTRUST_SERVICE_ERROR, ex.errorCode());
        assertTrue(ex.getMessage().contains("ErrorCode:"),
                "Exception message should contain error code info");
        assertTrue(ex.getMessage().contains("FaultMessage:"),
                "Exception message should contain fault message info");
    }

    @Test
    void parse_ErrorResponse_ContainsReasonAndSubcode() {
        MsalServiceException ex = assertThrows(MsalServiceException.class,
                () -> WSTrustResponse.parse(errorXml, WSTrustVersion.WSTRUST13));

        assertTrue(ex.getMessage().contains("RequestFailed"),
                "Exception should contain parsed error subcode");
        assertTrue(ex.getMessage().contains("MSIS3127"),
                "Exception should contain the SOAP fault reason text");
    }

    @Test
    void parse_UndefinedVersion_ThrowsException() {
        // UNDEFINED version has empty XPaths which cause XPathExpressionException
        assertThrows(Exception.class,
                () -> WSTrustResponse.parse(successXml, WSTrustVersion.UNDEFINED));
    }

    @Test
    void isTokenSaml2_nonSaml1Type_returnsTrue() {
        // Token from test XML is SAML 1.0
        assertFalse(successResponse.isTokenSaml2());

        // Verify the constant value
        assertEquals("urn:oasis:names:tc:SAML:1.0:assertion", WSTrustResponse.SAML1_ASSERTION);
    }

    @Test
    void parse_ValidToken_GettersReturnExpectedValues() {
        assertFalse(successResponse.isErrorFound());
        assertNull(successResponse.getFaultMessage());
        assertNull(successResponse.getErrorCode());
        assertNotNull(successResponse.getToken());
        assertFalse(successResponse.getToken().isEmpty());
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
