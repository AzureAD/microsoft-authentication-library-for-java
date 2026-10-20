// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class TokenRequestRegressionTest {
    private static final String CLIENT = "00000000-0000-0000-0000-000000000002";
    private static final String TENANT = "00000000-0000-0000-0000-000000000001";
    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TENANT;
    private static final Set<String> SCOPES = Collections.singleton("api://local-test/.default");
    private static final String CHALLENGE =
            "{\"access_token\":{\"acr\":{\"essential\":true,\"value\":\"cm1\"}}}";

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void clientCredentialLookupAfterOnBehalfOf(boolean includeAccount) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication app = confidentialApplication(endpoint, false);

        IAuthenticationResult delegated = app.acquireToken(
                OnBehalfOfParameters.builder(SCOPES, new UserAssertion("local-user-a")).build())
                .get(10, TimeUnit.SECONDS);
        assertEquals("local-delegated-token", delegated.accessToken());
        assertEquals(1, endpoint.forms.size());
        assertEquals("on_behalf_of", endpoint.lastForm().get("requested_token_use"));
        assertEquals("local-user-a", endpoint.lastForm().get("assertion"));
        assertEquals(includeAccount, delegated.account() != null);
        assertEquals(new UserAssertion("local-user-a").getAssertionHash(),
                app.tokenCache.accessTokens.values().iterator().next().userAssertionHash());
        String snapshot = app.tokenCache().serialize();

        IAuthenticationResult selected = app.acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS);
        assertEquals("local-application-token", selected.accessToken());
        assertNull(selected.account());
        assertNull(selected.idToken());
        assertEquals(2, endpoint.forms.size());
        assertEquals("client_credentials", endpoint.lastForm().get("grant_type"));
        IAuthenticationResult cachedApp = app.acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS);
        assertEquals(selected.accessToken(), cachedApp.accessToken());
        assertNull(cachedApp.account());
        assertNull(cachedApp.idToken());
        assertEquals(TokenSource.CACHE, cachedApp.metadata().tokenSource());
        assertEquals(2, endpoint.forms.size());
        assertEquals(delegated.accessToken(), app.acquireToken(
                OnBehalfOfParameters.builder(SCOPES, new UserAssertion("local-user-a")).build())
                .get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(2, endpoint.forms.size());

        RecordingEndpoint restoredEndpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication restored = confidentialApplication(restoredEndpoint, false);
        restored.tokenCache().deserialize(snapshot);
        assertEquals("local-application-token", restored.acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(1, restoredEndpoint.forms.size());

        RecordingEndpoint forcedEndpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication forced = confidentialApplication(forcedEndpoint, false);
        forced.tokenCache().deserialize(snapshot);
        assertEquals("local-application-token", forced.acquireToken(
                ClientCredentialParameters.builder(SCOPES).skipCache(true).build())
                .get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(1, forcedEndpoint.forms.size());
        assertEquals("client_credentials", forcedEndpoint.lastForm().get("grant_type"));
        assertFalse(forcedEndpoint.lastForm().containsKey("assertion"));

        RecordingEndpoint cleanEndpoint = new RecordingEndpoint(includeAccount);
        assertEquals("local-application-token", confidentialApplication(cleanEndpoint, false).acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(1, cleanEndpoint.forms.size());
        assertEquals("client_credentials", cleanEndpoint.lastForm().get("grant_type"));

        RecordingEndpoint otherUserEndpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication otherUser = confidentialApplication(otherUserEndpoint, false);
        otherUser.tokenCache().deserialize(snapshot);
        otherUser.acquireToken(OnBehalfOfParameters.builder(SCOPES, new UserAssertion("local-user-b")).build())
                .get(10, TimeUnit.SECONDS);
        assertEquals(1, otherUserEndpoint.forms.size());
        assertEquals("local-user-b", otherUserEndpoint.lastForm().get("assertion"));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void onBehalfOfClaimsWithCapabilities(boolean capabilities) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(false);
        ConfidentialClientApplication app = confidentialApplication(endpoint, capabilities);

        app.acquireToken(OnBehalfOfParameters.builder(SCOPES, new UserAssertion("local-user-a"))
                .claims(challenge()).skipCache(true).build()).get(10, TimeUnit.SECONDS);

        assertEquals(1, endpoint.forms.size());
        assertEquals("urn:ietf:params:oauth:grant-type:jwt-bearer", endpoint.lastForm().get("grant_type"));
        assertEquals("on_behalf_of", endpoint.lastForm().get("requested_token_use"));
        assertClaims(endpoint.lastForm(), true, capabilities);
    }

    @Test
    void clientCredentialLookupAfterAccountBoundRefresh() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        ConfidentialClientApplication app = confidentialApplication(endpoint, false);
        IAuthenticationResult seeded = app.acquireToken(
                RefreshTokenParameters.builder(SCOPES, "local-seed-refresh").build())
                .get(10, TimeUnit.SECONDS);
        assertNotNull(seeded.account());
        assertNull(app.tokenCache.accessTokens.values().iterator().next().userAssertionHash());

        IAuthenticationResult selected = app.acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS);

        assertEquals("local-application-token", selected.accessToken());
        assertNull(selected.account());
        assertNull(selected.idToken());
        assertEquals(2, endpoint.forms.size());
        assertEquals("client_credentials", endpoint.lastForm().get("grant_type"));
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void warmOnBehalfOfLookupWithClaims(boolean capabilities, boolean includeAccount) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication app = confidentialApplication(endpoint, capabilities);
        UserAssertion assertion = new UserAssertion("local-user-a");
        app.acquireToken(OnBehalfOfParameters.builder(SCOPES, assertion).build())
                .get(10, TimeUnit.SECONDS);

        IAuthenticationResult selected = app.acquireToken(OnBehalfOfParameters.builder(SCOPES, assertion)
                .claims(challenge()).build()).get(10, TimeUnit.SECONDS);

        assertEquals("local-delegated-token", selected.accessToken());
        assertEquals(2, endpoint.forms.size());
        assertEquals("local-user-a", endpoint.lastForm().get("assertion"));
        assertClaims(endpoint.lastForm(), true, capabilities);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void warmClientCredentialLookupWithClaims(boolean capabilities) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(false);
        ConfidentialClientApplication app = confidentialApplication(endpoint, capabilities);
        String seededToken = app.acquireToken(ClientCredentialParameters.builder(SCOPES).build())
                .get(10, TimeUnit.SECONDS).accessToken();

        IAuthenticationResult selected = app.acquireToken(ClientCredentialParameters.builder(SCOPES)
                .claims(challenge()).build()).get(10, TimeUnit.SECONDS);

        assertEquals(seededToken, selected.accessToken());
        assertEquals(2, endpoint.forms.size());
        assertEquals("client_credentials", endpoint.lastForm().get("grant_type"));
        assertClaims(endpoint.lastForm(), true, capabilities);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void accountlessRefreshRequirementsDoNotReturnCachedToken(boolean forceRefresh) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(false);
        ConfidentialClientApplication app = confidentialApplication(endpoint, false);
        app.acquireToken(ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS);
        if (!forceRefresh) {
            app.tokenCache.accessTokens.values().iterator().next()
                    .refreshOn(Long.toString(System.currentTimeMillis() / 1000 - 60));
        }

        ExecutionException failure = assertThrows(ExecutionException.class, () -> app.acquireTokenSilently(
                SilentParameters.builder(SCOPES).forceRefresh(forceRefresh).build()).get(10, TimeUnit.SECONDS));
        assertEquals(AuthenticationErrorCode.CACHE_MISS,
                assertInstanceOf(MsalClientException.class, failure.getCause()).errorCode());
        assertEquals(1, endpoint.forms.size());

        app.acquireToken(ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS);
        assertEquals(forceRefresh ? 1 : 2, endpoint.forms.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void assertionEntriesCoexistAfterSerialization(boolean includeAccount) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication app = confidentialApplication(endpoint, false);
        UserAssertion first = new UserAssertion("local-user-a");
        UserAssertion second = new UserAssertion("local-user-b");
        app.acquireToken(OnBehalfOfParameters.builder(SCOPES, first).build()).get(10, TimeUnit.SECONDS);
        app.acquireToken(OnBehalfOfParameters.builder(SCOPES, second).build()).get(10, TimeUnit.SECONDS);
        assertEquals(2, app.tokenCache.accessTokens.size());

        RecordingEndpoint restoredEndpoint = new RecordingEndpoint(includeAccount);
        ConfidentialClientApplication restored = confidentialApplication(restoredEndpoint, false);
        restored.tokenCache().deserialize(app.tokenCache().serialize());
        restored.acquireToken(OnBehalfOfParameters.builder(SCOPES, first).build()).get(10, TimeUnit.SECONDS);
        restored.acquireToken(OnBehalfOfParameters.builder(SCOPES, second).build()).get(10, TimeUnit.SECONDS);
        assertEquals(0, restoredEndpoint.forms.size());
        if (includeAccount) {
            assertTrue(restored.tokenCache.refreshTokens.values().stream()
                    .allMatch(token -> token.userAssertionHash() != null));
        }
    }

    @Test
    void refreshedAssertionEntryReplacesLegacyKey() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(false);
        ConfidentialClientApplication app = confidentialApplication(endpoint, false);
        UserAssertion assertion = new UserAssertion("local-user-a");
        app.acquireToken(OnBehalfOfParameters.builder(SCOPES, assertion).build()).get(10, TimeUnit.SECONDS);
        String key = app.tokenCache.accessTokens.keySet().iterator().next();
        String legacyKey = key.replace("-" + assertion.getAssertionHash().toLowerCase(), "");
        assertNotEquals(key, legacyKey);

        ConfidentialClientApplication restored = confidentialApplication(endpoint, false);
        restored.tokenCache().deserialize(app.tokenCache().serialize().replace(key, legacyKey));
        restored.acquireToken(OnBehalfOfParameters.builder(SCOPES, assertion).claims(challenge()).build())
                .get(10, TimeUnit.SECONDS);
        assertEquals(1, restored.tokenCache.accessTokens.size());
        assertTrue(restored.tokenCache.accessTokens.containsKey(key));
        assertFalse(restored.tokenCache.accessTokens.containsKey(legacyKey));
        assertClaims(endpoint.lastForm(), true, false);
    }

    @Test
    void appTokenProviderReceivesClaimsJson() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = ConfidentialClientApplication.builder(CLIENT,
                        ClientCredentialFactory.createFromSecret("local-secret"))
                .authority(AUTHORITY)
                .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                .httpClient(request -> { throw new AssertionError("Unexpected HTTP request"); })
                .appTokenProvider(parameters -> {
                    calls.incrementAndGet();
                    assertEquals(JsonHelper.parseJsonToMap(CHALLENGE),
                            JsonHelper.parseJsonToMap(parameters.getClaims()));
                    TokenProviderResult result = new TokenProviderResult();
                    result.setAccessToken("local-provider-token");
                    result.setTenantId(TENANT);
                    result.setExpiresInSeconds(3600);
                    return CompletableFuture.completedFuture(result);
                })
                .build();

        assertEquals("local-provider-token", app.acquireToken(
                ClientCredentialParameters.builder(SCOPES).claims(challenge()).build())
                .get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(1, calls.get());
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void authorizationCodeClaimsAndAccountIsolation(boolean publicClient, boolean capabilities) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        AbstractClientApplicationBase app = publicClient
                ? publicApplication(endpoint, capabilities) : confidentialApplication(endpoint, capabilities);
        app.acquireToken(AuthorizationCodeParameters.builder("local-code", URI.create("https://localhost/redirect"))
                .scopes(SCOPES).claims(challenge()).build()).get(10, TimeUnit.SECONDS);
        assertClaims(endpoint.lastForm(), true, capabilities);
        assertEquals("authorization_code", endpoint.lastForm().get("grant_type"));
        if (!publicClient) {
            IAuthenticationResult applicationToken = ((ConfidentialClientApplication) app).acquireToken(
                    ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS);
            assertEquals("local-application-token", applicationToken.accessToken());
            assertNull(applicationToken.account());
            assertEquals(2, endpoint.forms.size());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void userFicClaimsSurviveCachedRefresh(boolean capabilities) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        ConfidentialClientApplication app = confidentialApplication(endpoint, capabilities);
        app.acquireToken(UserFederatedIdentityCredentialParameters.builder(SCOPES, "local-user-a", "local-fic")
                .claims(challenge()).build()).get(10, TimeUnit.SECONDS);
        assertClaims(endpoint.lastForm(), true, capabilities);
        assertEquals("user_fic", endpoint.lastForm().get("grant_type"));
        app.acquireToken(UserFederatedIdentityCredentialParameters.builder(SCOPES, "local-user-a", "local-fic")
                .claims(challenge()).build()).get(10, TimeUnit.SECONDS);
        assertEquals(2, endpoint.forms.size());
        assertEquals("refresh_token", endpoint.lastForm().get("grant_type"));
        assertClaims(endpoint.lastForm(), true, capabilities);
        assertEquals("local-application-token", app.acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(3, endpoint.forms.size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void deviceCodeClaimsMergeWithCapabilities(boolean capabilities) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        PublicClientApplication app = publicApplication(endpoint, capabilities);
        AtomicInteger deviceCodes = new AtomicInteger();
        app.acquireToken(DeviceCodeFlowParameters.builder(SCOPES, deviceCode -> deviceCodes.incrementAndGet())
                .claims(challenge()).build()).get(10, TimeUnit.SECONDS);
        assertEquals(1, deviceCodes.get());
        assertEquals(1, endpoint.forms.size());
        assertEquals(GrantConstants.DEVICE_CODE, endpoint.lastForm().get("grant_type"));
        assertClaims(endpoint.lastForm(), true, capabilities);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void silentRefreshWithoutIdentityMetadataRetainsAccount(boolean onBehalfOf) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        ConfidentialClientApplication app = confidentialApplication(endpoint, false);
        UserAssertion assertion = new UserAssertion("local-user-a");
        IAuthenticationResult seeded = onBehalfOf
                ? app.acquireToken(OnBehalfOfParameters.builder(SCOPES, assertion).build()).get(10, TimeUnit.SECONDS)
                : app.acquireToken(RefreshTokenParameters.builder(SCOPES, "local-seed-refresh").build())
                        .get(10, TimeUnit.SECONDS);
        endpoint.includeAccount = false;
        IAuthenticationResult refreshed = app.acquireTokenSilently(
                SilentParameters.builder(SCOPES, seeded.account()).claims(challenge()).build())
                .get(10, TimeUnit.SECONDS);
        assertNotNull(refreshed.account());
        assertEquals(seeded.account().homeAccountId(), refreshed.account().homeAccountId());
        assertClaims(endpoint.lastForm(), true, false);
        assertEquals(1, app.tokenCache.accessTokens.size());
        assertEquals(onBehalfOf ? assertion.getAssertionHash() : null,
                app.tokenCache.accessTokens.values().iterator().next().userAssertionHash());
        IAuthenticationResult cached = app.acquireTokenSilently(
                SilentParameters.builder(SCOPES, seeded.account()).build()).get(10, TimeUnit.SECONDS);
        assertEquals(refreshed.accessToken(), cached.accessToken());
        assertEquals(2, endpoint.forms.size());
        if (onBehalfOf) {
            assertEquals(refreshed.accessToken(), app.acquireToken(
                    OnBehalfOfParameters.builder(SCOPES, assertion).build()).get(10, TimeUnit.SECONDS).accessToken());
            assertEquals(2, endpoint.forms.size());
        }
        assertEquals("local-application-token", app.acquireToken(
                ClientCredentialParameters.builder(SCOPES).build()).get(10, TimeUnit.SECONDS).accessToken());
        assertEquals(3, endpoint.forms.size());
    }

    @Test
    void interactionRequiredCacheDistinguishesClaims() throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        PublicClientApplication app = publicApplication(endpoint, false);
        IAuthenticationResult seed = app.acquireToken(
                RefreshTokenParameters.builder(SCOPES, "local-seed-refresh").build()).get(10, TimeUnit.SECONDS);
        endpoint.interactionRequired = true;
        try {
            SilentParameters first = SilentParameters.builder(SCOPES, seed.account()).claims(challenge()).build();
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> app.acquireTokenSilently(first).get(10, TimeUnit.SECONDS));
            assertInstanceOf(MsalInteractionRequiredException.class, failure.getCause());
            assertThrows(ExecutionException.class,
                    () -> app.acquireTokenSilently(first).get(10, TimeUnit.SECONDS));
            assertEquals(2, endpoint.forms.size());
            endpoint.interactionRequired = false;
            ClaimsRequest second = ClaimsRequest.formatAsClaimsRequest(
                    "{\"access_token\":{\"acr\":{\"essential\":true,\"value\":\"cm2\"}}}");
            app.acquireTokenSilently(SilentParameters.builder(SCOPES, seed.account()).claims(second).build())
                    .get(10, TimeUnit.SECONDS);
            assertEquals(3, endpoint.forms.size());
            assertEquals(JsonHelper.parseJsonToMap(second.formatAsJSONString()),
                    JsonHelper.parseJsonToMap(endpoint.lastForm().get("claims")));
        } finally {
            InteractionRequiredCache.clear();
        }
    }

    @ParameterizedTest
    @CsvSource({"false, false", "false, true", "true, false", "true, true"})
    void silentRefreshClaimsAndDirectRefreshControl(boolean publicClient, boolean capabilities) throws Exception {
        RecordingEndpoint endpoint = new RecordingEndpoint(true);
        AbstractClientApplicationBase app = publicClient
                ? publicApplication(endpoint, capabilities)
                : confidentialApplication(endpoint, capabilities);
        IAuthenticationResult seeded = app.acquireToken(
                RefreshTokenParameters.builder(SCOPES, "local-seed-refresh").build())
                .get(10, TimeUnit.SECONDS);
        assertNotNull(seeded.account());
        assertEquals(1, app.getAccounts().get(10, TimeUnit.SECONDS).size());
        assertEquals(1, endpoint.forms.size());

        IAuthenticationResult cached = app.acquireTokenSilently(
                SilentParameters.builder(SCOPES, seeded.account()).build()).get(10, TimeUnit.SECONDS);
        assertEquals(seeded.accessToken(), cached.accessToken());
        assertEquals(1, endpoint.forms.size());

        IAuthenticationResult refreshed = app.acquireTokenSilently(
                SilentParameters.builder(SCOPES, seeded.account()).claims(challenge()).build())
                .get(10, TimeUnit.SECONDS);
        assertNotEquals(seeded.accessToken(), refreshed.accessToken());
        assertEquals(2, endpoint.forms.size());
        assertEquals("refresh_token", endpoint.lastForm().get("grant_type"));
        assertEquals("local-refresh-token", endpoint.lastForm().get("refresh_token"));
        assertEquals(!publicClient, endpoint.lastForm().containsKey("client_secret"));
        assertEquals(CacheRefreshReason.CLAIMS, refreshed.metadata().cacheRefreshReason());
        assertClaims(endpoint.lastForm(), true, capabilities);

        app.acquireToken(RefreshTokenParameters.builder(SCOPES, "local-refresh-token")
                .claims(challenge()).build()).get(10, TimeUnit.SECONDS);
        assertEquals(3, endpoint.forms.size());
        assertEquals("refresh_token", endpoint.lastForm().get("grant_type"));
        assertClaims(endpoint.lastForm(), true, capabilities);
    }

    private static ClaimsRequest challenge() {
        return ClaimsRequest.formatAsClaimsRequest(CHALLENGE);
    }

    private static void assertClaims(Map<String, String> form, boolean challenge, boolean capabilities) {
        String claims = form.get("claims");
        if (!challenge && !capabilities) {
            assertNull(claims);
            return;
        }
        assertNotNull(claims);
        Map<?, ?> accessTokenClaims = assertInstanceOf(Map.class,
                JsonHelper.parseJsonToMap(claims).get("access_token"));
        assertEquals(challenge, accessTokenClaims.containsKey("acr"));
        assertEquals(capabilities, accessTokenClaims.containsKey("xms_cc"));
        if (challenge) {
            assertEquals(JsonHelper.parseJsonToMap(CHALLENGE).get("access_token"),
                    Collections.singletonMap("acr", accessTokenClaims.get("acr")));
        }
        if (capabilities) {
            assertEquals(Collections.singletonMap("values", Collections.singletonList("cp1")),
                    accessTokenClaims.get("xms_cc"));
        }
    }

    private static ConfidentialClientApplication confidentialApplication(IHttpClient endpoint, boolean capabilities)
            throws MalformedURLException {
        ConfidentialClientApplication.Builder builder = ConfidentialClientApplication.builder(CLIENT,
                        ClientCredentialFactory.createFromSecret("local-secret"))
                .authority(AUTHORITY)
                .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                .httpClient(endpoint);
        if (capabilities) {
            builder.clientCapabilities(Collections.singleton("cp1"));
        }
        return builder.build();
    }

    private static PublicClientApplication publicApplication(IHttpClient endpoint, boolean capabilities)
            throws MalformedURLException {
        PublicClientApplication.Builder builder = PublicClientApplication.builder(CLIENT)
                .authority(AUTHORITY)
                .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                .httpClient(endpoint);
        if (capabilities) {
            builder.clientCapabilities(Collections.singleton("cp1"));
        }
        return builder.build();
    }

    private static class RecordingEndpoint implements IHttpClient {
        private boolean includeAccount;
        private boolean interactionRequired;
        private final List<Map<String, String>> forms = new ArrayList<>();

        RecordingEndpoint(boolean includeAccount) {
            this.includeAccount = includeAccount;
        }

        @Override
        public IHttpResponse send(HttpRequest request) {
            assertEquals(HttpMethod.POST, request.httpMethod());
            if (request.url().toString().equals(AUTHORITY + "/oauth2/v2.0/devicecode")) {
                return TestHelper.expectedResponse(200,
                        "{\"device_code\":\"local-device-code\",\"user_code\":\"local-user-code\","
                                + "\"verification_uri\":\"https://localhost/verify\",\"expires_in\":600,\"interval\":0}");
            }
            assertEquals("https://login.microsoftonline.com/" + TENANT + "/oauth2/v2.0/token", request.url().toString());
            Map<String, String> form = StringHelper.parseQueryParameters(request.body());
            forms.add(form);
            if (interactionRequired) {
                return TestHelper.expectedResponse(400,
                        "{\"error\":\"invalid_grant\",\"error_description\":\"local interaction required\"}");
            }
            String token;
            switch (form.get("grant_type")) {
                case "urn:ietf:params:oauth:grant-type:jwt-bearer":
                    assertEquals("on_behalf_of", form.get("requested_token_use"));
                    token = "local-delegated-token";
                    break;
                case "client_credentials":
                    token = "local-application-token";
                    break;
                case "refresh_token":
                    token = "local-refreshed-token-" + forms.size();
                    break;
                case "authorization_code":
                case "user_fic":
                case "device_code":
                    token = "local-user-token";
                    break;
                default:
                    throw new AssertionError("Unexpected local grant: " + form.get("grant_type"));
            }
            HashMap<String, String> claims = new HashMap<>();
            claims.put("oid", "local-user-a");
            claims.put("sub", "local-user-a");
            claims.put("tid", TENANT);
            claims.put("preferred_username", "local-user-a");
            String info = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    ("{\"uid\":\"local-user-a\",\"utid\":\"" + TENANT + "\"}").getBytes(StandardCharsets.UTF_8));
            String body = "{\"access_token\":\"" + token + "\",\"expires_in\":3600,\"token_type\":\"Bearer\","
                    + "\"scope\":\"api://local-test/.default\""
                    + (includeAccount && !"client_credentials".equals(form.get("grant_type"))
                    ? ",\"refresh_token\":\"local-refresh-token\",\"client_info\":\"" + info
                    + "\",\"id_token\":\"" + TestHelper.createIdToken(claims) + "\"" : "") + "}";
            return TestHelper.expectedResponse(200, body);
        }

        Map<String, String> lastForm() {
            return forms.get(forms.size() - 1);
        }
    }
}
