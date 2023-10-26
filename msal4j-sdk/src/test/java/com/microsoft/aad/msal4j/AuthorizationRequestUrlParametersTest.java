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
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

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
    void testBuilder_optionalParameters() throws UnsupportedEncodingException {
        Set<String> clientCapabilities = new HashSet<>();
        clientCapabilities.add("llt");
        clientCapabilities.add("ssm");

        PublicClientApplication app = PublicClientApplication.builder("client_id").clientCapabilities(clientCapabilities).build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .extraScopesToConsent(new LinkedHashSet<>(Arrays.asList("extraScopeToConsent1", "extraScopeToConsent2")))
                        .responseMode(ResponseMode.QUERY)
                        .codeChallenge("challenge")
                        .codeChallengeMethod("method")
                        .state("app_state")
                        .nonce("app_nonce")
                        .correlationId("corr_id")
                        .loginHint("hint")
                        .domainHint("domain_hint")
                        .claimsChallenge("{\"id_token\":{\"auth_time\":{\"essential\":true}},\"access_token\":{\"auth_time\":{\"essential\":true}}}")
                        .prompt(Prompt.SELECT_ACCOUNT)
                        .build();

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

        assertEquals(queryParameters.get("scope"),
                "openid profile offline_access scope extraScopeToConsent1 extraScopeToConsent2");
        assertEquals(queryParameters.get("response_type"), "code");
        assertEquals(queryParameters.get("redirect_uri"), "http://localhost:8080");
        assertEquals(queryParameters.get("client_id"), "client_id");
        assertEquals(queryParameters.get("prompt"), "select_account");
        assertEquals(queryParameters.get("response_mode"), "query");
        assertEquals(queryParameters.get("code_challenge"), "challenge");
        assertEquals(queryParameters.get("code_challenge_method"), "method");
        assertEquals(queryParameters.get("state"), "app_state");
        assertEquals(queryParameters.get("nonce"), "app_nonce");
        assertEquals(queryParameters.get("correlation_id"), "corr_id");
        assertEquals(queryParameters.get("login_hint"), "hint");
        assertEquals(queryParameters.get("domain_hint"), "domain_hint");
        assertEquals(queryParameters.get("claims"), "{\"id_token\":{\"auth_time\":{\"essential\":true}},\"access_token\":{\"auth_time\":{\"essential\":true},\"xms_cc\":{\"values\":[\"llt\",\"ssm\"]}}}");

        // CCS routing
        assertEquals(queryParameters.get(HttpHeaders.X_ANCHOR_MAILBOX), String.format(HttpHeaders.X_ANCHOR_MAILBOX_UPN_FORMAT, "hint"));
    }
}
