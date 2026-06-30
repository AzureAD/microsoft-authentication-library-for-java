// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the per-request {@code claimsFromClient(String)} API across confidential-client flows
 * (client credentials and on-behalf-of). Client-originated claims differ from server-issued
 * {@code claims} challenges: they are forwarded on the wire as a standard OAuth {@code claims}
 * parameter and cause cache isolation keyed on the claims value (rather than bypassing the cache).
 *
 * @see FmiTest for the analogous fmi_path cache-isolation tests
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientClaimsTest {

    private static final String CLAIMS_A = "{\"claimA\":{\"essential\":true}}";
    private static final String CLAIMS_B = "{\"claimB\":{\"values\":[\"v1\"]}}";

    private ConfidentialClientApplication buildCca(DefaultHttpClient httpClientMock) throws Exception {
        return ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                .authority("https://login.microsoftonline.com/tenant/")
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();
    }

    @SafeVarargs
    private final DefaultHttpClient mockHttpClient(HashMap<String, String>... responses) throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        if (responses.length == 0) {
            when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                    TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                            TestHelper.getSuccessfulTokenResponse(new HashMap<>())));
        } else {
            org.mockito.stubbing.OngoingStubbing<IHttpResponse> stub =
                    when(httpClientMock.send(any(HttpRequest.class)));
            for (HashMap<String, String> response : responses) {
                stub = stub.thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(response)));
            }
        }
        return httpClientMock;
    }

    private HashMap<String, String> tokenResponse(String accessToken) {
        HashMap<String, String> values = new HashMap<>();
        values.put("access_token", accessToken);
        return values;
    }

    // ========================================================================
    // JSON object validation (JsonHelper.validateJsonObjectFormat)
    // ========================================================================

    @Test
    void validateJsonObjectFormat_acceptsSingleObject_rejectsEverythingElse() {
        // Valid single JSON objects pass.
        assertDoesNotThrow(() -> JsonHelper.validateJsonObjectFormat(CLAIMS_A));
        assertDoesNotThrow(() -> JsonHelper.validateJsonObjectFormat("{\"xms_az_nwperimid\":{\"essential\":true}}"));

        // Rejected: malformed input, valid-but-non-object JSON (array/scalar), and a valid object
        // followed by trailing tokens (e.g. "{}{}") which is not a single well-formed JSON value.
        for (String invalid : new String[]{
                "not json at all", "[1,2,3]", "\"justAString\"",
                "{\"a\":1} garbage", "{}{}", "{},123"}) {
            MsalClientException ex = assertThrows(MsalClientException.class,
                    () -> JsonHelper.validateJsonObjectFormat(invalid));
            assertEquals(AuthenticationErrorCode.INVALID_JSON, ex.errorCode());
        }
    }

    // ========================================================================
    // Builder behavior — blank is a no-op, invalid JSON throws
    // ========================================================================

    @Test
    void builders_blankClaims_areNoOp() {
        ClientCredentialParameters cc = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .claimsFromClient("   ")
                .build();
        assertNull(cc.clientClaims());
        assertEquals("", cc.computeExtCacheKeyHash());

        OnBehalfOfParameters obo = OnBehalfOfParameters
                .builder(Collections.singleton("scope"), new UserAssertion(TestHelper.signedAssertion))
                .claimsFromClient(null)
                .build();
        assertNull(obo.clientClaims());
        assertEquals("", obo.computeExtCacheKeyHash());

        ManagedIdentityParameters mi = ManagedIdentityParameters
                .builder("resource")
                .claimsFromClient("")
                .build();
        assertNull(mi.clientClaims());
        assertEquals("", mi.computeExtCacheKeyHash());

        UserFederatedIdentityCredentialParameters fic = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton("scope"), "user@contoso.com", "assertion")
                .claimsFromClient("   ")
                .build();
        assertNull(fic.clientClaims());
        assertEquals("", fic.computeExtCacheKeyHash());

        AuthorizationCodeParameters ac = AuthorizationCodeParameters
                .builder("code", URI.create("https://localhost/redirect"))
                .scopes(Collections.singleton("scope"))
                .claimsFromClient(null)
                .build();
        assertNull(ac.clientClaims());
        assertEquals("", ac.computeExtCacheKeyHash());
    }

    @Test
    void builders_invalidClaims_throwInvalidJson() {
        assertEquals(AuthenticationErrorCode.INVALID_JSON, assertThrows(MsalClientException.class, () ->
                ClientCredentialParameters.builder(Collections.singleton("scope")).claimsFromClient("nope")).errorCode());
        assertEquals(AuthenticationErrorCode.INVALID_JSON, assertThrows(MsalClientException.class, () ->
                OnBehalfOfParameters.builder(Collections.singleton("scope"), new UserAssertion(TestHelper.signedAssertion)).claimsFromClient("[1]")).errorCode());
        assertEquals(AuthenticationErrorCode.INVALID_JSON, assertThrows(MsalClientException.class, () ->
                ManagedIdentityParameters.builder("resource").claimsFromClient("nope")).errorCode());
        assertEquals(AuthenticationErrorCode.INVALID_JSON, assertThrows(MsalClientException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(Collections.singleton("scope"), "user@contoso.com", "assertion").claimsFromClient("nope")).errorCode());
        assertEquals(AuthenticationErrorCode.INVALID_JSON, assertThrows(MsalClientException.class, () ->
                AuthorizationCodeParameters.builder("code", URI.create("https://localhost/redirect")).claimsFromClient("nope")).errorCode());
    }

    @Test
    void builders_invalidClaims_exceptionMessageDoesNotLeakPayload() {
        // The claims payload may contain sensitive data and must never appear in the error message.
        String secret = "{\"sensitive_secret_value\":";
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                ClientCredentialParameters.builder(Collections.singleton("scope")).claimsFromClient(secret));
        assertFalse(ex.getMessage().contains("sensitive_secret_value"));
    }

    @Test
    void clientClaims_distinctValues_produceDistinctCacheKeyHashes() {
        ClientCredentialParameters a = ClientCredentialParameters
                .builder(Collections.singleton("scope")).claimsFromClient(CLAIMS_A).build();
        ClientCredentialParameters b = ClientCredentialParameters
                .builder(Collections.singleton("scope")).claimsFromClient(CLAIMS_B).build();

        assertNotEquals("", a.computeExtCacheKeyHash());
        assertNotEquals(a.computeExtCacheKeyHash(), b.computeExtCacheKeyHash());
    }

    // ========================================================================
    // Client credentials — wire and cache isolation
    // ========================================================================

    @Test
    void clientCredential_clientClaims_sentAsClaimsBodyParam() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient();
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("https://graph.microsoft.com/.default"))
                .claimsFromClient(CLAIMS_A)
                .build();

        cca.acquireToken(parameters).get();

        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("claims=") && body.contains("claimA")
                    && body.contains("grant_type=client_credentials");
        }));
    }

    @Test
    void clientCredential_serverClaimsAndClientClaims_areMergedOnWire() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient();
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        ClaimsRequest serverClaims = new ClaimsRequest();
        serverClaims.requestClaimInAccessToken("given_name", new RequestedClaimAdditionalInfo(true, null, null));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .claims(serverClaims)
                .claimsFromClient(CLAIMS_A)
                .build();

        cca.acquireToken(parameters).get();

        // Both the server-issued claim (given_name) and the client claim (claimA) appear in the single
        // merged OAuth "claims" parameter — client claims do not replace server claims, they merge in.
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("claims=") && body.contains("given_name") && body.contains("claimA");
        }));
    }

    @Test
    void clientClaims_overrideServerClaims_onLeafConflict() {
        // On a conflicting leaf key the client-supplied value wins; nested objects deep-merge.
        // This is the precedence behavior used when claims() and claimsFromClient() overlap.
        assertEquals("{\"a\":2}", JsonHelper.mergeJSONString("{\"a\":1}", "{\"a\":2}"));
        assertTrue(JsonHelper.mergeJSONString("{\"o\":{\"x\":1}}", "{\"o\":{\"y\":2}}").contains("\"x\":1"));
        assertTrue(JsonHelper.mergeJSONString("{\"o\":{\"x\":1}}", "{\"o\":{\"y\":2}}").contains("\"y\":2"));
    }

    @Test
    void clientCredential_distinctClaims_isolateCache() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient(tokenResponse("token_A"), tokenResponse("token_B"));
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        IAuthenticationResult resultA = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton("scope")).claimsFromClient(CLAIMS_A).build()).get();
        IAuthenticationResult resultB = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton("scope")).claimsFromClient(CLAIMS_B).build()).get();

        assertEquals("token_A", resultA.accessToken());
        assertEquals("token_B", resultB.accessToken());
        assertEquals(2, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(2)).send(any());
    }

    @Test
    void clientCredential_sameClaims_cacheHit() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient();
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scope")).claimsFromClient(CLAIMS_A).build();

        IAuthenticationResult result1 = cca.acquireToken(params).get();
        IAuthenticationResult result2 = cca.acquireToken(params).get();

        assertEquals(result1.accessToken(), result2.accessToken());
        assertEquals(1, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(1)).send(any());
    }

    @Test
    void clientCredential_claimsDoNotCollideWithNonClaimsTokens() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient(tokenResponse("regular"), tokenResponse("with_claims"));
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton("scope")).build()).get();
        cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton("scope")).claimsFromClient(CLAIMS_A).build()).get();

        assertEquals(2, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(2)).send(any());
    }

    // ========================================================================
    // On-behalf-of — wire and cache isolation
    // ========================================================================

    @Test
    void onBehalfOf_clientClaims_sentAsClaimsBodyParam() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient();
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        OnBehalfOfParameters parameters = OnBehalfOfParameters
                .builder(Collections.singleton("scope"), new UserAssertion(TestHelper.signedAssertion))
                .claimsFromClient(CLAIMS_A)
                .build();

        cca.acquireToken(parameters).get();

        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("claims=") && body.contains("claimA");
        }));
    }

    @Test
    void onBehalfOf_distinctClaims_isolateCache() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient(tokenResponse("obo_A"), tokenResponse("obo_B"));
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        UserAssertion assertion = new UserAssertion(TestHelper.signedAssertion);

        IAuthenticationResult resultA = cca.acquireToken(OnBehalfOfParameters
                .builder(Collections.singleton("scope"), assertion).claimsFromClient(CLAIMS_A).build()).get();
        IAuthenticationResult resultB = cca.acquireToken(OnBehalfOfParameters
                .builder(Collections.singleton("scope"), assertion).claimsFromClient(CLAIMS_B).build()).get();

        assertEquals("obo_A", resultA.accessToken());
        assertEquals("obo_B", resultB.accessToken());
        assertEquals(2, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(2)).send(any());
    }

    @Test
    void onBehalfOf_sameClaims_cacheHit() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient();
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        OnBehalfOfParameters params = OnBehalfOfParameters
                .builder(Collections.singleton("scope"), new UserAssertion(TestHelper.signedAssertion))
                .claimsFromClient(CLAIMS_A)
                .build();

        IAuthenticationResult result1 = cca.acquireToken(params).get();
        IAuthenticationResult result2 = cca.acquireToken(params).get();

        assertEquals(result1.accessToken(), result2.accessToken());
        assertEquals(1, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(1)).send(any());
    }

    // ========================================================================
    // Authorization code (confidential client / web app) — wire
    // ========================================================================

    @Test
    void authorizationCode_clientClaims_sentAsClaimsBodyParam() throws Exception {
        DefaultHttpClient httpClientMock = mockHttpClient();
        ConfidentialClientApplication cca = buildCca(httpClientMock);

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("auth-code-123", URI.create("https://localhost/redirect"))
                .scopes(Collections.singleton("scope"))
                .claimsFromClient(CLAIMS_A)
                .build();

        cca.acquireToken(parameters).get();

        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("claims=") && body.contains("claimA")
                    && body.contains("grant_type=authorization_code");
        }));
    }
}
