// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.apache.commons.text.StringEscapeUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
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

        assertEquals(WSTrustRequest.escapeXMLElementData(DATA_TO_ESCAPE), XML_ESCAPED_DATA);
        assertEquals(WSTrustRequest.escapeXMLElementData(DATA_TO_ESCAPE),
                StringEscapeUtils.escapeXml10(DATA_TO_ESCAPE));
    }
}
