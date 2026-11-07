// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AgeAssuranceTargetValidationTest {
    private static final java.util.Set<String> SCOPES = Collections.singleton("scope");

    @ParameterizedTest
    @ValueSource(strings = {"+mc,-sm", "-SM,+MC", "-mc,-sm", "+sm,+mc"})
    void acceptsExactIdentitiesIndependentOfStatusSignCaseAndOrder(String receipt) {
        AgeAssuranceTargetValidation validation = AgeAssuranceTargetValidation.fromClaims(claims("MC", "sm"));
        for (int status : new int[]{0, 1, 2}) {
            assertDoesNotThrow(() -> validation.validateIdToken(token(receipt, status)));
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "+mc", "+mc,+sm,+other", "+mc,+wrong", "+mc,-mc",
            "+mc,+MC,+sm", "mc,+sm", "+mc,sm", "+mc,+sm,", ",+mc,+sm", "+mc,,+sm",
            "++mc,+sm", "+-mc,+sm", "+mc, +sm", "+mc,+sm ", "+mc,+", "-,+sm",
            "+mc,+sm\u00A0", "+mc,+sm\uFEFF"})
    void rejectsMissingMalformedOrDifferentReceiptsEvenForZeroStatus(String receipt) {
        assertMismatch(() -> AgeAssuranceTargetValidation.fromClaims(claims("mc", "sm"))
                .validateIdToken(token(receipt, 0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "true", "[\"+mc\"]", "{\"value\":\"+mc\"}"})
    void rejectsNonStringReceipt(String receiptJson) {
        assertMismatch(() -> AgeAssuranceTargetValidation.fromClaims(claims("mc"))
                .validateIdToken(encodedToken("{\"xms_agvs\":0,\"xms_agvt\":" + receiptJson + "}")));
    }

    @Test
    void allIsLiteralAndFutureIdentitiesAreNotAllowlisted() {
        AgeAssuranceTargetValidation all = AgeAssuranceTargetValidation.fromClaims(claims("all"));
        assertDoesNotThrow(() -> all.validateIdToken(token("-ALL", 0)));
        assertMismatch(() -> all.validateIdToken(token("+mc,+sm", 1)));
        AgeAssuranceTargetValidation future = AgeAssuranceTargetValidation.fromClaims(claims("future.v2", "a-b_c", "\u672A\u6765"));
        assertDoesNotThrow(() -> future.validateIdToken(token("-A-B_C,+FUTURE.V2,+\u672A\u6765", 0)));
    }

    @Test
    void absentOrAccessTokenOnlyRequestsDoNotInspectTokens() {
        for (String claims : Arrays.asList(null, "{}", "{\"id_token\":{\"xms_agvs\":null}}",
                "{\"id_token\":{\"xms_agvs\":{\"additionalProperties\":[\"interrupt-if-not-verified\"]}}}",
                "{\"id_token\":{\"xms_agvs\":{\"additionalProperties\":[null,0,true]}}}",
                "{\"id_token\":{\"xms_agvs\":{\"additionalProperties\":{\"other\":true}}}}",
                claims("mc").replace("id_token", "access_token"))) {
            AgeAssuranceTargetValidation validation = AgeAssuranceTargetValidation.fromClaims(claims);
            assertFalse(validation.isRequired());
            assertDoesNotThrow(() -> validation.validateIdToken(null));
            assertDoesNotThrow(() -> validation.validateIdToken("not-a-token"));
        }
    }

    @Test
    void requestTargetsUseExactKeysCaseInsensitivePrefixAndSetSemantics() {
        String request = claims("mc", "MC").replace("target-mc", "TARGET-mc");
        assertDoesNotThrow(() -> AgeAssuranceTargetValidation.fromClaims(request).validateIdToken(token("-mc", 0)));
        assertFalse(AgeAssuranceTargetValidation.fromClaims(claims("mc").replace("id_token", "ID_TOKEN")).isRequired());
        assertFalse(AgeAssuranceTargetValidation.fromClaims(claims("mc").replace("target-mc", " target-mc")).isRequired());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "mc ", " mc", "+mc", "-mc", "mc,sm", "mc\t"})
    void doesNotDropMalformedRequestedTargets(String identity) throws Exception {
        RequestedClaimAdditionalInfo info = new RequestedClaimAdditionalInfo(false, null, null);
        info.setAdditionalProperties(Collections.singletonList("target-" + identity));
        ClaimsRequest request = new ClaimsRequest();
        request.requestClaimInIdToken("xms_agvs", info);
        assertMismatch(() -> AgeAssuranceTargetValidation.fromClaims(request.formatAsJSONString()));
    }

    @Test
    void rawAndTypedNetworkRequestsFailBeforeAnyCacheWrite() throws Exception {
        for (boolean raw : new boolean[]{true, false}) {
            PublicClientApplication app = application(request -> response(token("+wrong", 0)));
            String before = app.tokenCache().serialize();
            AuthorizationCodeParameters.AuthorizationCodeParametersBuilder builder = AuthorizationCodeParameters
                    .builder("code", URI.create("https://localhost")).scopes(SCOPES);
            if (raw) {
                builder.claimsFromClient(claims("mc"));
            } else {
                builder.claims(ClaimsRequest.formatAsClaimsRequest(claims("mc")));
            }
            assertFutureMismatch(app.acquireToken(builder.build()));
            assertEquals(before, app.tokenCache().serialize());
        }
    }

    @Test
    void networkMissingReceiptAndAccessTokenOnlyNoOp() throws Exception {
        PublicClientApplication app = application(request -> response(token(null, 0)));
        assertFutureMismatch(app.acquireToken(authorizationCode(claims("mc"))));
        assertTrue(app.tokenCache().accessTokens.isEmpty());
        assertNotNull(app.acquireToken(authorizationCode(claims("mc").replace("id_token", "access_token"))).join());
    }

    @Test
    void actualSilentCacheResultRejectsMismatchWithoutNetwork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        PublicClientApplication app = application(request -> {
            calls.incrementAndGet();
            return response(token("+mc", 0));
        });
        IAuthenticationResult initial = app.acquireToken(AuthorizationCodeParameters
                .builder("code", URI.create("https://localhost")).scopes(SCOPES)
                .claims(ClaimsRequest.formatAsClaimsRequest(claims("mc"))).build()).join();
        assertNotNull(initial.account());
        SilentParameters matching = SilentParameters.builder(SCOPES, initial.account())
                .extraQueryParameters(Collections.singletonMap("claims", claims("MC"))).build();
        assertEquals(initial.idToken(), app.acquireTokenSilently(matching).join().idToken());
        SilentParameters wrong = SilentParameters.builder(SCOPES, initial.account())
                .extraQueryParameters(Collections.singletonMap("claims", claims("sm"))).build();
        assertFutureMismatch(app.acquireTokenSilently(wrong));
        assertEquals(1, calls.get());
    }

    @Test
    void silentRefreshFailurePreservesExistingCache() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        PublicClientApplication app = application(request -> response(token(
                calls.getAndIncrement() == 0 ? "+mc" : "+sm", 0)));
        IAuthenticationResult initial = app.acquireToken(authorizationCode(claims("mc"))).join();
        String before = app.tokenCache().serialize();
        assertFutureMismatch(app.acquireTokenSilently(SilentParameters.builder(SCOPES, initial.account())
                .claims(ClaimsRequest.formatAsClaimsRequest(claims("mc"))).build()));
        assertEquals(before, app.tokenCache().serialize());
        assertEquals(2, calls.get());
    }

    @ParameterizedTest
    @ValueSource(strings = {"obo", "user-fic"})
    void nestedConfidentialCacheDoesNotSwallowReceiptMismatch(String flow) throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder("client", ClientCredentialFactory.createFromSecret("synthetic-secret"))
                .authority("https://login.microsoftonline.com/tenant")
                .instanceDiscovery(false).validateAuthority(false)
                .httpClient(request -> response(token(calls.getAndIncrement() == 0 ? "+mc" : "+sm", 0)))
                .build();
        assertNotNull(acquireConfidential(app, flow, "mc", true).join());
        assertNotNull(acquireConfidential(app, flow, "sm", true).join());
        String before = app.tokenCache().serialize();
        assertFutureMismatch(acquireConfidential(app, flow, "mc", false));
        assertEquals(2, calls.get());
        assertEquals(before, app.tokenCache().serialize());
    }

    @Test
    void appProviderMissingIdTokenFailsBeforeCacheButAccessTokenOnlyIsUnchanged() throws Exception {
        TokenProviderResult provided = new TokenProviderResult();
        provided.setAccessToken("opaque-access-token");
        provided.setExpiresInSeconds(3600);
        provided.setTenantId("tenant");
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder("client", ClientCredentialFactory.createFromSecret("synthetic-secret"))
                .authority("https://login.microsoftonline.com/tenant")
                .instanceDiscovery(false).validateAuthority(false)
                .appTokenProvider(parameters -> CompletableFuture.completedFuture(provided)).build();
        String before = app.tokenCache().serialize();
        assertFutureMismatch(app.acquireToken(ClientCredentialParameters.builder(SCOPES).skipCache(true)
                .claims(ClaimsRequest.formatAsClaimsRequest(claims("mc"))).build()));
        assertEquals(before, app.tokenCache().serialize());
        assertEquals("opaque-access-token", app.acquireToken(ClientCredentialParameters.builder(SCOPES)
                .skipCache(true).claims(ClaimsRequest.formatAsClaimsRequest(
                        claims("mc").replace("id_token", "access_token"))).build()).join().accessToken());
    }

    @Test
    void clientCredentialCacheMissingIdTokenDoesNotFallBackToNetwork() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder("client", ClientCredentialFactory.createFromSecret("synthetic-secret"))
                .authority("https://login.microsoftonline.com/tenant")
                .instanceDiscovery(false).validateAuthority(false).httpClient(request -> {
                    calls.incrementAndGet();
                    return httpResponse("{\"access_token\":\"opaque-access-token\",\"expires_in\":3600,"
                            + "\"scope\":\"scope\",\"token_type\":\"Bearer\"}");
                }).build();
        assertNotNull(app.acquireToken(ClientCredentialParameters.builder(SCOPES).skipCache(true).build()).join());
        assertFutureMismatch(app.acquireToken(ClientCredentialParameters.builder(SCOPES).skipCache(false)
                .extraQueryParameters(Collections.singletonMap("claims", claims("mc"))).build()));
        assertEquals(1, calls.get());
    }

    @Test
    void deviceCodeSendsTargetAndRejectsResponseBeforeCache() throws Exception {
        List<String> claimsOnWire = new ArrayList<>();
        PublicClientApplication app = application(request -> {
            claimsOnWire.add(StringHelper.parseQueryParameters(request.body()).get("claims"));
            if (request.url().getPath().endsWith("devicecode")) {
                return httpResponse("{\"device_code\":\"device-code\",\"user_code\":\"code\","
                        + "\"verification_uri\":\"https://example.com\",\"expires_in\":60,\"interval\":0}");
            }
            return response(token("+sm", 0));
        });
        assertFutureMismatch(app.acquireToken(DeviceCodeFlowParameters.builder(SCOPES, code -> { })
                .claims(ClaimsRequest.formatAsClaimsRequest(claims("mc"))).build()));
        assertEquals(2, claimsOnWire.size());
        for (String requestClaims : claimsOnWire) {
            assertTrue(requestClaims.contains("target-mc"));
        }
        assertTrue(app.tokenCache().accessTokens.isEmpty());
    }

    @Test
    void brokerInteractiveSilentAndPasswordResultsAreBoundToRequest() throws Exception {
        IBroker broker = mock(IBroker.class);
        when(broker.isBrokerAvailable()).thenReturn(true);
        IAuthenticationResult wrong = AuthenticationResult.builder().idToken(token("+sm", 0)).build();
        when(broker.acquireToken(any(), any(InteractiveRequestParameters.class)))
                .thenReturn(CompletableFuture.completedFuture(wrong));
        when(broker.acquireToken(any(), any(SilentParameters.class)))
                .thenReturn(CompletableFuture.completedFuture(wrong));
        when(broker.acquireToken(any(), any(UserNamePasswordParameters.class)))
                .thenReturn(CompletableFuture.completedFuture(wrong));
        PublicClientApplication app = PublicClientApplication.builder("client").broker(broker).build();
        assertFutureMismatch(app.acquireToken(InteractiveRequestParameters.builder(URI.create("http://localhost"))
                .claimsChallenge(claims("mc")).scopes(SCOPES).build()));
        assertFutureMismatch(app.acquireTokenSilently(SilentParameters.builder(SCOPES)
                .claims(ClaimsRequest.formatAsClaimsRequest(claims("mc"))).build()));
        assertFutureMismatch(app.acquireToken(UserNamePasswordParameters.builder(SCOPES,
                "user@example.com", new char[]{'t'}).claims(ClaimsRequest.formatAsClaimsRequest(claims("mc"))).build()));
    }

    @Test
    void browserChallengeAndTypedTargetsReachRedemptionAndFailBeforeCache() throws Exception {
        for (boolean raw : new boolean[]{true, false}) {
            AtomicInteger authorizations = new AtomicInteger();
            AtomicInteger redemptions = new AtomicInteger();
            PublicClientApplication app = application(request -> {
                assertTrue(StringHelper.parseQueryParameters(request.body()).get("claims").contains("target-mc"));
                redemptions.incrementAndGet();
                return response(token("+sm", 0));
            });
            InteractiveRequestParameters.InteractiveRequestParametersBuilder builder = InteractiveRequestParameters
                    .builder(URI.create("http://localhost")).scopes(SCOPES)
                    .systemBrowserOptions(SystemBrowserOptions.builder().openBrowserAction(url -> {
                        Map<String, String> query = StringHelper.parseQueryParameters(url.getQuery());
                        assertTrue(query.get("claims").contains("target-mc"));
                        authorizations.incrementAndGet();
                        completeBrowserCallback(query);
                    }).build());
            if (raw) {
                builder.claimsChallenge(claims("mc"));
            } else {
                builder.claims(ClaimsRequest.formatAsClaimsRequest(claims("mc")));
            }
            assertFutureMismatch(app.acquireToken(builder.build()));
            assertEquals(1, authorizations.get());
            assertEquals(1, redemptions.get());
            assertTrue(app.tokenCache().accessTokens.isEmpty());
            assertTrue(app.tokenCache().idTokens.isEmpty());
        }
    }

    @Test
    void overlappingBrokerOperationsCannotAcceptEachOthersReceipts() throws Exception {
        IBroker broker = mock(IBroker.class);
        when(broker.isBrokerAvailable()).thenReturn(true);
        CompletableFuture<IAuthenticationResult> first = new CompletableFuture<>();
        CompletableFuture<IAuthenticationResult> second = new CompletableFuture<>();
        when(broker.acquireToken(any(), any(SilentParameters.class))).thenReturn(first, second);
        PublicClientApplication app = PublicClientApplication.builder("client").broker(broker).build();
        ClaimsRequest mutable = ClaimsRequest.formatAsClaimsRequest(claims("mc"));
        CompletableFuture<IAuthenticationResult> mc = app.acquireTokenSilently(
                SilentParameters.builder(SCOPES).claims(mutable).build());
        mutable.getIdTokenRequestedClaims().get(0).getRequestedClaimAdditionalInfo()
                .setAdditionalProperties(Collections.singletonList("target-sm"));
        CompletableFuture<IAuthenticationResult> sm = app.acquireTokenSilently(
                SilentParameters.builder(SCOPES).claims(mutable).build());
        first.complete(AuthenticationResult.builder().idToken(token("+sm", 0)).build());
        second.complete(AuthenticationResult.builder().idToken(token("+mc", 0)).build());
        assertFutureMismatch(mc);
        assertFutureMismatch(sm);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "not-a-token", "header..signature", "header.bad!.signature",
            "header.e30.signature", "header.e30.signature.extra"})
    void rejectsAbsentOrMalformedIdTokensWithoutLeakingParserDetails(String token) {
        assertMismatch(() -> AgeAssuranceTargetValidation.fromClaims(claims("mc")).validateIdToken(token));
    }

    @Test
    void overlappingOperationsCaptureClaimsBeforeAsyncExecution() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch blocked = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    blocked.countDown();
                    try {
                        assertTrue(release.await(10, TimeUnit.SECONDS));
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(ex);
                    }
                });
            }
            assertTrue(blocked.await(10, TimeUnit.SECONDS));
            PublicClientApplication app = PublicClientApplication.builder("client")
                    .instanceDiscovery(false).validateAuthority(false).executorService(executor)
                    .httpClient(request -> {
                        String sent = StringHelper.parseQueryParameters(request.body()).get("claims");
                        return response(token(sent.contains("target-mc") ? "+mc" : "+sm", 0));
                    }).build();
            ClaimsRequest mutable = ClaimsRequest.formatAsClaimsRequest(claims("mc"));
            CompletableFuture<IAuthenticationResult> mc = app.acquireToken(AuthorizationCodeParameters
                    .builder("code", URI.create("https://localhost")).scopes(SCOPES).claims(mutable).build());
            mutable.getIdTokenRequestedClaims().get(0).getRequestedClaimAdditionalInfo()
                    .setAdditionalProperties(Collections.singletonList("target-sm"));
            CompletableFuture<IAuthenticationResult> sm = app.acquireToken(AuthorizationCodeParameters
                    .builder("code2", URI.create("https://localhost")).scopes(SCOPES).claims(mutable).build());
            release.countDown();
            assertEquals(token("+mc", 0), mc.get(10, TimeUnit.SECONDS).idToken());
            assertEquals(token("+sm", 0), sm.get(10, TimeUnit.SECONDS).idToken());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    private static PublicClientApplication application(IHttpClient client) throws Exception {
        return PublicClientApplication.builder("client").instanceDiscovery(false).validateAuthority(false)
                .httpClient(client).build();
    }

    private static void completeBrowserCallback(Map<String, String> query) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(query.get("redirect_uri")).openConnection();
            try {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                byte[] body = ("code=code&state=" + query.get("state")).getBytes(StandardCharsets.UTF_8);
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(body);
                }
                assertEquals(200, connection.getResponseCode());
                connection.getInputStream().close();
            } finally {
                connection.disconnect();
            }
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private static AuthorizationCodeParameters authorizationCode(String claims) {
        return AuthorizationCodeParameters.builder("code", URI.create("https://localhost"))
                .scopes(SCOPES).claimsFromClient(claims).build();
    }

    private static CompletableFuture<IAuthenticationResult> acquireConfidential(
            ConfidentialClientApplication app, String flow, String target, boolean skipCache) {
        if ("obo".equals(flow)) {
            return app.acquireToken(OnBehalfOfParameters.builder(SCOPES, new UserAssertion("synthetic-assertion"))
                    .skipCache(skipCache).claimsFromClient(claims(target)).build());
        }
        return app.acquireToken(UserFederatedIdentityCredentialParameters
                .builder(SCOPES, "user@example.com", "synthetic-assertion").forceRefresh(skipCache)
                .claimsFromClient(claims(target)).build());
    }

    static String claims(String... identities) {
        return "{\"id_token\":{\"xms_agvs\":{\"additionalProperties\":[\"interrupt-if-not-verified\",\"target-"
                + String.join("\",\"target-", identities) + "\"]}}}";
    }

    private static String token(String receipt, int status) {
        return encodedToken("{\"sub\":\"subject\",\"tid\":\"tenant\",\"oid\":\"user\","
                + "\"preferred_username\":\"user@example.com\",\"xms_agvs\":" + status
                + (receipt == null ? "" : ",\"xms_agvt\":\"" + receipt + "\"") + "}");
    }

    private static String encodedToken(String payload) {
        return "header." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + ".signature";
    }

    private static IHttpResponse response(String idToken) {
        return httpResponse("{\"access_token\":\"opaque-access-token\",\"id_token\":\"" + idToken
                + "\",\"refresh_token\":\"test-refresh-token\",\"expires_in\":3600,\"scope\":\"scope\","
                + "\"client_info\":\"eyJ1aWQiOiJ1c2VyIiwidXRpZCI6InRlbmFudCJ9\",\"token_type\":\"Bearer\"}");
    }

    private static IHttpResponse httpResponse(String body) {
        HttpResponse response = new HttpResponse();
        response.statusCode(200);
        response.body(body);
        response.addHeaders(Collections.singletonMap("Content-Type", Collections.singletonList("application/json")));
        return response;
    }

    private static void assertMismatch(org.junit.jupiter.api.function.Executable action) {
        MsalClientException error = assertThrows(MsalClientException.class, action);
        assertEquals(AuthenticationErrorCode.AGE_ASSURANCE_TARGET_MISMATCH, error.errorCode());
        assertEquals("The ID token does not match the requested age-assurance targets.", error.getMessage());
    }

    private static void assertFutureMismatch(CompletableFuture<IAuthenticationResult> future) {
        CompletionException error = assertThrows(CompletionException.class, future::join);
        assertInstanceOf(MsalClientException.class, error.getCause());
        assertEquals(AuthenticationErrorCode.AGE_ASSURANCE_TARGET_MISMATCH,
                ((MsalClientException) error.getCause()).errorCode());
    }
}
