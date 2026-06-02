// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuthorizationRequestUrlParametersTest {

    @Test
    void testBuilder_onlyRequiredParameters() throws UnsupportedEncodingException {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        Map<String, String> extraParameters = new HashMap<>();
        extraParameters.put("id_token_hint", "test");
        extraParameters.put("another_param", "some_value");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .extraQueryParameters(extraParameters)
                        .build();

        assertEquals(ResponseMode.FORM_POST, parameters.responseMode());
        assertEquals(redirectUri, parameters.redirectUri());
        assertEquals(4, parameters.scopes().size());
        assertEquals(2, parameters.extraQueryParameters.size());

        assertNull(parameters.loginHint());
        assertNull(parameters.codeChallenge());
        assertNull(parameters.codeChallengeMethod());
        assertNull(parameters.correlationId());
        assertNull(parameters.nonce());
        assertNull(parameters.prompt());
        assertNull(parameters.state());

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);

        assertEquals("login.microsoftonline.com", authorizationUrl.getHost());
        assertEquals("/common/oauth2/v2.0/authorize", authorizationUrl.getPath());

        Map<String, String> queryParameters = new HashMap<>();
        String query = authorizationUrl.getQuery();

        String[] queryPairs = query.split("&");
        for (String pair : queryPairs) {
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }

        assertEquals("openid profile offline_access scope", queryParameters.get("scope"));
        assertEquals("code", queryParameters.get("response_type"));
        assertEquals("http://localhost:8080", queryParameters.get("redirect_uri"));
        assertEquals("client_id", queryParameters.get("client_id"));
        assertEquals("form_post", queryParameters.get("response_mode"));
        assertEquals("test", queryParameters.get("id_token_hint"));
    }

    @Test
    void testBuilder_invalidRequiredParameters() {
        String redirectUri = "";
        Set<String> scope = Collections.singleton("scope");

        assertThrows(IllegalArgumentException.class, () ->
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .build());
    }

    @Test
    void testBuilder_conflictingParameters() {
        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        Map<String, String> extraParameters = new HashMap<>();
        extraParameters.put("scope", "scope");

        AuthorizationRequestUrlParameters
                .builder(redirectUri, scope)
                .extraQueryParameters(extraParameters)
                .build();
    }

    @Test
    void testBuilder_responseMode() throws UnsupportedEncodingException {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .responseMode(ResponseMode.QUERY) // This should be overridden to FORM_POST
                        .build();

        assertEquals(ResponseMode.FORM_POST, parameters.responseMode());
        assertEquals(redirectUri, parameters.redirectUri());
        assertEquals(4, parameters.scopes().size());

        assertNull(parameters.loginHint());
        assertNull(parameters.codeChallenge());
        assertNull(parameters.codeChallengeMethod());
        assertNull(parameters.correlationId());
        assertNull(parameters.nonce());
        assertNull(parameters.prompt());
        assertNull(parameters.state());

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);

        assertEquals("login.microsoftonline.com", authorizationUrl.getHost());
        assertEquals("/common/oauth2/v2.0/authorize", authorizationUrl.getPath());

        Map<String, String> queryParameters = new HashMap<>();
        String query = authorizationUrl.getQuery();

        String[] queryPairs = query.split("&");
        for (String pair : queryPairs) {
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }

        assertEquals("openid profile offline_access scope", queryParameters.get("scope"));
        assertEquals("code", queryParameters.get("response_type"));
        assertEquals("http://localhost:8080", queryParameters.get("redirect_uri"));
        assertEquals("client_id", queryParameters.get("client_id"));
        assertEquals("form_post", queryParameters.get("response_mode"));
    }
}
