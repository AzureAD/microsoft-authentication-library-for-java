// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WSTrustRequestTest {

    @Test
    void buildMessage_cloudAudienceUrnNotNull() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", "username",
                "password", WSTrustVersion.WSTRUST2005, "cloudAudienceUrn").toString();

        assertTrue(msg.contains("<a:EndpointReference><a:Address>cloudAudienceUrn</a:Address></a:EndpointReference>"));
    }

    @Test
    void buildMessage_cloudAudienceUrnNull() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", "username",
                "password", WSTrustVersion.WSTRUST2005, null).toString();

        assertTrue(msg.contains("<a:EndpointReference><a:Address>" + WSTrustRequest.DEFAULT_APPLIES_TO + "</a:Address></a:EndpointReference>"));
    }

    @Test
    void buildMessage_cloudAudienceUrnEmpty() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", "username",
                "password", WSTrustVersion.WSTRUST2005, "").toString();

        assertTrue(msg.contains("<a:EndpointReference><a:Address>" + WSTrustRequest.DEFAULT_APPLIES_TO + "</a:Address></a:EndpointReference>"));
    }

    @Test
    void buildMessage_integrated() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", null,
                null, WSTrustVersion.WSTRUST13, "cloudAudienceUrn").toString();

        assertTrue(msg.contains("<a:EndpointReference><a:Address>cloudAudienceUrn</a:Address></a:EndpointReference>"));
        assertTrue(!msg.contains("<o:Security s:mustUnderstand"));
    }

    @Test
    void escapeXMLElementDataTest() {
        String DATA_TO_ESCAPE = "o_!as & a34~'fe<> \" a1";
        String XML_ESCAPED_DATA = "o_!as &amp; a34~&apos;fe&lt;&gt; &quot; a1";

        assertEquals(XML_ESCAPED_DATA, WSTrustRequest.escapeXMLElementData(DATA_TO_ESCAPE));
        assertEquals(StringEscapeUtils.escapeXml10(DATA_TO_ESCAPE),
                WSTrustRequest.escapeXMLElementData(DATA_TO_ESCAPE));
    }

    @Test
    void buildMessage_wsTrust13_usesTrust13Namespaces() {
        String msg = WSTrustRequest.buildMessage("https://adfs.example.com", "user",
                "pass", WSTrustVersion.WSTRUST13, "urn:federation:MicrosoftOnline").toString();

        assertTrue(msg.contains("http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue"),
                "WSTrust 1.3 should use trust/200512 action");
        assertTrue(msg.contains("xmlns:trust='http://docs.oasis-open.org/ws-sx/ws-trust/200512'"),
                "WSTrust 1.3 should use trust/200512 namespace");
        assertTrue(msg.contains("http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer"),
                "WSTrust 1.3 should use trust/200512 Bearer key type");
    }

    @Test
    void buildMessage_wsTrust2005_uses2005Namespaces() {
        String msg = WSTrustRequest.buildMessage("https://adfs.example.com", "user",
                "pass", WSTrustVersion.WSTRUST2005, "urn:federation:MicrosoftOnline").toString();

        assertTrue(msg.contains("http://schemas.xmlsoap.org/ws/2005/02/trust/RST/Issue"),
                "WSTrust 2005 should use ws/2005 action");
        assertTrue(msg.contains("xmlns:trust='http://schemas.xmlsoap.org/ws/2005/02/trust'"),
                "WSTrust 2005 should use ws/2005 namespace");
        assertTrue(msg.contains("http://schemas.xmlsoap.org/ws/2005/05/identity/NoProofKey"),
                "WSTrust 2005 should use NoProofKey");
    }

    @Test
    void buildMessage_withCredentials_includesSecurityHeader() {
        String msg = WSTrustRequest.buildMessage("address", "testuser",
                "testpass", WSTrustVersion.WSTRUST13, null).toString();

        assertTrue(msg.contains("<o:Security s:mustUnderstand='1'"),
                "Should include security header when credentials provided");
        assertTrue(msg.contains("<o:Username>testuser</o:Username>"));
        assertTrue(msg.contains("<o:Password>testpass</o:Password>"));
        assertTrue(msg.contains("<u:Created>"));
        assertTrue(msg.contains("<u:Expires>"));
    }

    @Test
    void buildMessage_withSpecialCharsInCredentials_escapesXml() {
        String msg = WSTrustRequest.buildMessage("address", "user&<>",
                "pass'\"", WSTrustVersion.WSTRUST13, null).toString();

        assertTrue(msg.contains("user&amp;&lt;&gt;"), "Username should be XML-escaped");
        assertTrue(msg.contains("pass&apos;&quot;"), "Password should be XML-escaped");
    }

    @Test
    void escapeXMLElementData_noSpecialChars_returnsUnchanged() {
        assertEquals("simple text 123", WSTrustRequest.escapeXMLElementData("simple text 123"));
    }

    @Test
    void escapeXMLElementData_emptyString() {
        assertEquals("", WSTrustRequest.escapeXMLElementData(""));
    }
}
