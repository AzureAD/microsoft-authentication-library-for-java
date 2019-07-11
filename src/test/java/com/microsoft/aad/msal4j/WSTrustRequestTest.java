// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = { "checkin" })
public class WSTrustRequestTest {

    @Test
    public void buildMessage_cloudAudienceUrnNotNull() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", "username",
                "password", WSTrustVersion.WSTRUST2005, "cloudAudienceUrn").toString();

        Assert.assertTrue(msg.contains("<a:EndpointReference><a:Address>cloudAudienceUrn</a:Address></a:EndpointReference>"));
    }

    @Test
    public void buildMessage_cloudAudienceUrnNull() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", "username",
                "password", WSTrustVersion.WSTRUST2005, null).toString();

        Assert.assertTrue(msg.contains("<a:EndpointReference><a:Address>" + WSTrustRequest.DEFAULT_APPLIES_TO + "</a:Address></a:EndpointReference>"));
    }

    @Test
    public void buildMessage_cloudAudienceUrnEmpty() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", "username",
                "password", WSTrustVersion.WSTRUST2005, "").toString();

        Assert.assertTrue(msg.contains("<a:EndpointReference><a:Address>" + WSTrustRequest.DEFAULT_APPLIES_TO + "</a:Address></a:EndpointReference>"));
    }
    
    @Test
    public void buildMessage_integrated() throws Exception {
        String msg = WSTrustRequest.buildMessage("address", null,
                null, WSTrustVersion.WSTRUST13, "cloudAudienceUrn").toString();

        Assert.assertTrue(msg.contains("<a:EndpointReference><a:Address>cloudAudienceUrn</a:Address></a:EndpointReference>"));
        Assert.assertTrue(!msg.contains("<o:Security s:mustUnderstand"));
    }
}
