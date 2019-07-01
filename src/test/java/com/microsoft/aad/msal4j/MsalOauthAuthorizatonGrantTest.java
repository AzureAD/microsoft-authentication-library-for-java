// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class MsalOauthAuthorizatonGrantTest {

    @Test
    public void testConstructor() {
        final OAuthAuthorizationGrant grant = new OAuthAuthorizationGrant(null,
                new HashMap<>());
        Assert.assertNotNull(grant);
    }

    @Test
    public void testToParameters() throws URISyntaxException {
        final OAuthAuthorizationGrant grant = new OAuthAuthorizationGrant(
                new AuthorizationCodeGrant(new AuthorizationCode("grant"),
                        new URI("http://microsoft.com")),
                (Map<String, List<String>>) null);
        Assert.assertNotNull(grant);
        Assert.assertNotNull(grant.toParameters());
    }
}
