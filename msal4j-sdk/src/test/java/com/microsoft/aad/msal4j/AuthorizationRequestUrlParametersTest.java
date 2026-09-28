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

        assertEquals(parameters.responseMode(), ResponseMode.FORM_POST);
        assertEquals(parameters.redirectUri(), redirectUri);
        assertEquals(parameters.scopes().size(), 4);
        assertEquals(parameters.extraQueryParameters.size(), 2);

        assertNull(parameters.loginHint());
        assertNull(parameters.codeChallenge());
        assertNull(parameters.codeChallengeMethod());
        assertNull(parameters.correlationId());
        assertNull(parameters.nonce());
        assertNull(parameters.prompt());
        assertNull(parameters.state());

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);

        assertEquals(authorizationUrl.getHost(), "login.microsoftonline.com");
        assertEquals(authorizationUrl.getPath(), "/common/oauth2/v2.0/authorize");

        Map<String, String> queryParameters = new HashMap<>();
        String query = authorizationUrl.getQuery();

        String[] queryPairs = query.split("&");
        for (String pair : queryPairs) {
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }

        assertEquals(queryParameters.get("scope"), "openid profile offline_access scope");
        assertEquals(queryParameters.get("response_type"), "code");
        assertEquals(queryParameters.get("redirect_uri"), "http://localhost:8080");
        assertEquals(queryParameters.get("client_id"), "client_id");
        assertEquals(queryParameters.get("response_mode"), "form_post");
        assertEquals(queryParameters.get("id_token_hint"),"test");
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
    void testBuilder_queryResponseModeIsOverriddenToFormPost() throws UnsupportedEncodingException {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        // Test that when QUERY is passed (deprecated), it's overridden to FORM_POST
        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .responseMode(ResponseMode.QUERY)  // Deprecated - should be overridden
                        .build();

        // Verify that the responseMode is overridden to FORM_POST
        assertEquals(ResponseMode.FORM_POST, parameters.responseMode(), 
                "ResponseMode.QUERY should be overridden to ResponseMode.FORM_POST");

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);

        Map<String, String> queryParameters = new HashMap<>();
        String query = authorizationUrl.getQuery();

        String[] queryPairs = query.split("&");
        for (String pair : queryPairs) {
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }

        // Verify that the actual response_mode parameter is "form_post", not "query"
        assertEquals("form_post", queryParameters.get("response_mode"), 
                "response_mode query parameter should be 'form_post' even when QUERY was specified");
    }
}
