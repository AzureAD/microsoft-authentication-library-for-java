// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

class AuthorizationRequestUrlParametersTest {

    @Test
    void testBuilder_onlyRequiredParameters() throws Exception {
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

        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

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
        // Verifies that duplicate parameter keys (extra query params overriding built-in params)
        // don't throw an exception — they log a warning and the extra value overwrites the built-in.
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
    void testBuilder_responseMode() throws Exception {
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

        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

        assertEquals("openid profile offline_access scope", queryParameters.get("scope"));
        assertEquals("code", queryParameters.get("response_type"));
        assertEquals("http://localhost:8080", queryParameters.get("redirect_uri"));
        assertEquals("client_id", queryParameters.get("client_id"));
        assertEquals("form_post", queryParameters.get("response_mode"));
    }

    @Test
    void testBuilder_allOptionalParams() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        String redirectUri = "http://localhost:8080";
        Set<String> scope = Collections.singleton("scope");

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder(redirectUri, scope)
                        .codeChallenge("challenge-value")
                        .codeChallengeMethod("S256")
                        .state("state-123")
                        .nonce("nonce-456")
                        .loginHint("user@contoso.com")
                        .domainHint("contoso.com")
                        .correlationId("corr-789")
                        .instanceAware(true)
                        .prompt(Prompt.CONSENT)
                        .responseMode(ResponseMode.FORM_POST)
                        .build();

        assertEquals("challenge-value", parameters.codeChallenge());
        assertEquals("S256", parameters.codeChallengeMethod());
        assertEquals("state-123", parameters.state());
        assertEquals("nonce-456", parameters.nonce());
        assertEquals("corr-789", parameters.correlationId());
        assertTrue(parameters.instanceAware());
        assertEquals(Prompt.CONSENT, parameters.prompt());
        assertEquals(ResponseMode.FORM_POST, parameters.responseMode());

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);
        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

        assertEquals("challenge-value", queryParameters.get("code_challenge"));
        assertEquals("S256", queryParameters.get("code_challenge_method"));
        assertEquals("state-123", queryParameters.get("state"));
        assertEquals("nonce-456", queryParameters.get("nonce"));
        assertEquals("user@contoso.com", queryParameters.get("login_hint"));
        assertEquals("contoso.com", queryParameters.get("domain_hint"));
        assertEquals("corr-789", queryParameters.get("correlation_id"));
        assertEquals("true", queryParameters.get("instance_aware"));
        assertEquals("consent", queryParameters.get("prompt"));
    }

    @Test
    void testBuilder_nullScopes_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                AuthorizationRequestUrlParameters.builder("http://localhost:8080", null));
    }

    @Test
    void testBuilder_extraScopesToConsent() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        Set<String> scope = Collections.singleton("User.Read");
        Set<String> extraScopes = new HashSet<>(Arrays.asList("Mail.Read", "Calendars.Read"));

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder("http://localhost:8080", scope)
                        .extraScopesToConsent(extraScopes)
                        .build();

        // Scopes should include common scopes + requested + extra
        Set<String> resultScopes = parameters.scopes();
        assertTrue(resultScopes.contains("User.Read"));
        assertTrue(resultScopes.contains("Mail.Read"));
        assertTrue(resultScopes.contains("Calendars.Read"));
        assertTrue(resultScopes.contains("openid"));

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);
        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

        String scopeParam = queryParameters.get("scope");
        assertTrue(scopeParam.contains("User.Read"));
        assertTrue(scopeParam.contains("Mail.Read"));
        assertTrue(scopeParam.contains("Calendars.Read"));
    }

    @Test
    void testBuilder_loginHint_SetsAnchorMailbox() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder("http://localhost:8080", Collections.singleton("scope"))
                        .loginHint("user@contoso.com")
                        .build();

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);
        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

        assertEquals("user@contoso.com", queryParameters.get("login_hint"));
        // X-AnchorMailbox should be set for CCS routing with UPN format
        assertEquals("upn:user@contoso.com", queryParameters.get("X-AnchorMailbox"));
    }

    @Test
    void testBuilder_formPostResponseMode_ExplicitlySet() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder("http://localhost:8080", Collections.singleton("scope"))
                        .responseMode(ResponseMode.FORM_POST)
                        .build();

        assertEquals(ResponseMode.FORM_POST, parameters.responseMode());

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);
        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

        assertEquals("form_post", queryParameters.get("response_mode"));
    }

    @Test
    void testBuilder_claimsChallenge() throws Exception {
        PublicClientApplication app = PublicClientApplication.builder("client_id").build();

        String claimsChallenge = "{\"access_token\":{\"nbf\":{\"essential\":true}}}";

        AuthorizationRequestUrlParameters parameters =
                AuthorizationRequestUrlParameters
                        .builder("http://localhost:8080", Collections.singleton("scope"))
                        .claimsChallenge(claimsChallenge)
                        .build();

        URL authorizationUrl = app.getAuthorizationRequestUrl(parameters);
        Map<String, String> queryParameters = parseQueryParameters(authorizationUrl);

        assertNotNull(queryParameters.get("claims"));
        assertTrue(queryParameters.get("claims").contains("nbf"));
    }

    private static Map<String, String> parseQueryParameters(URL url) throws Exception {
        Map<String, String> queryParameters = new HashMap<>();
        String query = url.getQuery();
        String[] queryPairs = query.split("&");
        for (String pair : queryPairs) {
            int idx = pair.indexOf("=");
            queryParameters.put(
                    URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
                    URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
        }
        return queryParameters;
    }
}
