// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *
 */
public class MsalOauthAuthorizatonGrantTest {

    @Test
    public void testConstructor() {
        final OAuthAuthorizationGrant grant = new OAuthAuthorizationGrant(null,
                new HashMap<>());
        assertNotNull(grant);
    }

    @Test
    public void testToParameters() throws URISyntaxException {
        final OAuthAuthorizationGrant grant = new OAuthAuthorizationGrant(
                new AuthorizationCodeGrant(new AuthorizationCode("grant"),
                        new URI("http://microsoft.com")),
                null);
        assertNotNull(grant);
        assertNotNull(grant.toParameters());
    }
}
