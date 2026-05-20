// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the User Federated Identity Credential (user_fic) flow.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserFederatedIdentityCredentialTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant/";
    private static final Set<String> SCOPES = Collections.singleton("https://graph.microsoft.com/.default");
    private static final String FAKE_ASSERTION = "fake.assertion.jwt";
    private static final String TEST_UPN = "user@contoso.com";
    private static final UUID TEST_OID = UUID.fromString("597f86cd-13f3-44c0-bece-a1e77ba43228");

    private DefaultHttpClient httpClientMock;
    private ConfidentialClientApplication cca;

    @BeforeEach
    void setUp() throws Exception {
        httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());
        cca = createCca(httpClientMock);
    }

    private ConfidentialClientApplication createCca(DefaultHttpClient httpClientMock) throws Exception {
        return ConfidentialClientApplication.builder(CLIENT_ID, ClientCredentialFactory.createFromSecret("secret"))
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();
    }

    private HttpResponse createSuccessResponse() {
        return createSuccessResponse(new HashMap<>());
    }

    private HttpResponse createSuccessResponse(HashMap<String, String> responseValues) {
        return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                TestHelper.getSuccessfulTokenResponse(responseValues));
    }

    private HttpResponse createSuccessResponseWithIdToken() {
        HashMap<String, String> responseValues = new HashMap<>();
        responseValues.put("id_token", TestHelper.ENCODED_JWT);
        return createSuccessResponse(responseValues);
    }

    // ========================================================================
    // Grant type and body parameters
    // ========================================================================

    @Test
    void userFic_SendsCorrectGrantType() throws Exception {
        // Arrange
        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — verify grant_type=user_fic in POST body
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("grant_type=user_fic");
        }));
    }

    // ========================================================================
    // user_federated_identity_credential body parameter
    // ========================================================================

    @Test
    void userFic_SendsAssertionInBody() throws Exception {
        // Arrange
        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — verify user_federated_identity_credential in POST body
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("user_federated_identity_credential=fake.assertion.jwt");
        }));
    }

    // ========================================================================
    // user_id / username body parameters — mutual exclusion
    // ========================================================================

    @Test
    void userFic_WithUpn_SendsUsernameNotUserId() throws Exception {
        // Arrange
        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — username present, user_id absent
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("username=user%40contoso.com")
                    && !body.contains("user_id=");
        }));
    }

    @Test
    void userFic_WithOid_SendsUserIdNotUsername() throws Exception {
        // Arrange
        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_OID, FAKE_ASSERTION)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — user_id present, username absent(as grant param, may appear in common params)
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("user_id=" + TEST_OID.toString())
                    && !body.contains("username=");
        }));
    }

    // ========================================================================
    // All parameters sent together
    // ========================================================================

    @Test
    void userFic_SendsAllOAuthParametersTogether() throws Exception {
        // Arrange
        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — all three key parameters present
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("grant_type=user_fic")
                    && body.contains("user_federated_identity_credential=fake.assertion.jwt")
                    && body.contains("username=user%40contoso.com")
                    && body.contains("client_info=1");
        }));
    }

    // ========================================================================
    // Scope augmentation: openid, offline_access, profile added
    // ========================================================================

    @Test
    void userFic_ScopeIncludesOidcScopes() throws Exception {
        // Arrange
        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — scope includes OIDC scopes added by OAuthAuthorizationGrant
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("openid")
                    && body.contains("offline_access")
                    && body.contains("profile");
        }));
    }

    // ========================================================================
    // Token stored in user cache
    // ========================================================================

    @Test
    void userFic_TokenStoredInUserCache() throws Exception {
        // Arrange — override default mock to return id_token
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponseWithIdToken());

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();

        // Act
        IAuthenticationResult result = cca.acquireToken(parameters).get();

        // Assert — account is present (token stored in user cache)
        assertNotNull(result.account(), "Result should have an account (user token cache)");

        Set<IAccount> accounts = cca.getAccounts().get();
        assertFalse(accounts.isEmpty(), "Accounts should be present in the cache");
    }

    // ========================================================================
    // Force refresh bypasses cache
    // ========================================================================

    @Test
    void userFic_ForceRefresh_BypassesCache() throws Exception {
        // Arrange
        String oid = "oid-user-1234";
        String tid = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";
        AtomicInteger callCount = new AtomicInteger(0);

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            callCount.incrementAndGet();
            return createUserResponse(oid, TEST_UPN, "access-token", tid);
        });

        // First call — populates cache (empty cache, goes to IdP)
        UserFederatedIdentityCredentialParameters params1 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();
        cca.acquireToken(params1).get();
        assertEquals(1, callCount.get(), "First call should hit IdP");

        // Second call with forceRefresh — should bypass cache despite matching cached account
        UserFederatedIdentityCredentialParameters params2 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();
        cca.acquireToken(params2).get();
        assertEquals(2, callCount.get(), "Second call with forceRefresh should hit IdP again");
    }

    // ========================================================================
    // Cache hit when not force-refreshing
    // ========================================================================

    @Test
    void userFic_CacheHit_WhenNotForceRefreshing() throws Exception {
        // Arrange
        String oid = "oid-user-1234";
        String tid = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";
        AtomicInteger callCount = new AtomicInteger(0);

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            callCount.incrementAndGet();
            return createUserResponse(oid, TEST_UPN, "access-token-cached", tid);
        });

        // First call — populates cache (cache is empty, so goes to IdP without needing forceRefresh)
        UserFederatedIdentityCredentialParameters params1 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .build();
        cca.acquireToken(params1).get();
        assertEquals(1, callCount.get(), "First call should hit IdP");

        // Second call without forceRefresh — should use cache (same UPN matches cached account)
        UserFederatedIdentityCredentialParameters params2 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(false)
                .build();
        cca.acquireToken(params2).get();
        assertEquals(1, callCount.get(), "Second call without forceRefresh should use cache");
    }

    // ========================================================================
    // Input validation
    // ========================================================================

    @Test
    void userFic_NullUsername_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, (String) null, FAKE_ASSERTION));
    }

    @Test
    void userFic_EmptyUsername_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, "", FAKE_ASSERTION));
    }

    @Test
    void userFic_NullAssertion_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, TEST_UPN, null));
    }

    @Test
    void userFic_EmptyAssertion_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, TEST_UPN, ""));
    }

    @Test
    void userFic_NullScopes_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(null, TEST_UPN, FAKE_ASSERTION));
    }

    @Test
    void userFic_NullOid_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, (UUID) null, FAKE_ASSERTION));
    }

    // ========================================================================
    // Parameters object validation
    // ========================================================================

    @Test
    void userFic_Parameters_UpnBuilder_SetsFieldsCorrectly() {
        UserFederatedIdentityCredentialParameters params = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertEquals(TEST_UPN, params.username());
        assertNull(params.userObjectId());
        assertEquals(FAKE_ASSERTION, params.assertion());
        assertTrue(params.forceRefresh());
    }

    @Test
    void userFic_Parameters_OidBuilder_SetsFieldsCorrectly() {
        UserFederatedIdentityCredentialParameters params = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_OID, FAKE_ASSERTION)
                .forceRefresh(false)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertNull(params.username());
        assertEquals(TEST_OID, params.userObjectId());
        assertEquals(FAKE_ASSERTION, params.assertion());
        assertFalse(params.forceRefresh());
    }

    // ========================================================================
    // Multi-user cache isolation
    // ========================================================================

    /**
     * Creates a token response with a specific user identity (oid + preferred_username).
     * This allows simulating different users in the token cache.
     */
    private HttpResponse createUserResponse(String oid, String preferredUsername, String accessToken, String tid) {
        // Build client_info: Base64URL({"uid":"<oid>","utid":"<tid>"})
        String clientInfoJson = String.format("{\"uid\":\"%s\",\"utid\":\"%s\"}", oid, tid);
        String clientInfo = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(clientInfoJson.getBytes(StandardCharsets.UTF_8));

        // Build id_token with the user's identity
        HashMap<String, String> idTokenValues = new HashMap<>();
        idTokenValues.put("oid", oid);
        idTokenValues.put("preferred_username", preferredUsername);
        idTokenValues.put("tid", tid);
        String idToken = TestHelper.createIdToken(idTokenValues);

        // Build full response
        HashMap<String, String> responseValues = new HashMap<>();
        responseValues.put("access_token", accessToken);
        responseValues.put("id_token", idToken);
        responseValues.put("client_info", clientInfo);

        return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                TestHelper.getSuccessfulTokenResponse(responseValues));
    }

    /**
     * Verifies that two different users (by UPN) acquire tokens via user_fic on the same CCA,
     * and AcquireTokenSilent returns the correct cached token for each user.
     */
    @Test
    void userFic_TwoUpns_SilentReturnsCorrectToken() throws Exception {
        // Arrange
        String aliceOid = "oid-alice-1111";
        String aliceUpn = "alice@contoso.com";
        String aliceToken = "access-token-alice";

        String bobOid = "oid-bob-2222";
        String bobUpn = "bob@contoso.com";
        String bobToken = "access-token-bob";

        String tid = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";

        AtomicReference<HttpResponse> nextResponse = new AtomicReference<>();

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> nextResponse.get());

        // Act: Acquire token for Alice
        nextResponse.set(createUserResponse(aliceOid, aliceUpn, aliceToken, tid));

        UserFederatedIdentityCredentialParameters aliceParams = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, aliceUpn, FAKE_ASSERTION)
                .build();
        IAuthenticationResult aliceResult = cca.acquireToken(aliceParams).get();
        assertEquals(aliceToken, aliceResult.accessToken());
        assertNotNull(aliceResult.account());

        // Act: Acquire token for Bob
        nextResponse.set(createUserResponse(bobOid, bobUpn, bobToken, tid));

        UserFederatedIdentityCredentialParameters bobParams = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, bobUpn, FAKE_ASSERTION)
                .build();
        IAuthenticationResult bobResult = cca.acquireToken(bobParams).get();
        assertEquals(bobToken, bobResult.accessToken());
        assertNotNull(bobResult.account());

        // Assert: Both accounts in cache
        Set<IAccount> accounts = cca.getAccounts().get();
        assertEquals(2, accounts.size(), "Two accounts should be cached");

        // Silent for Alice → should return Alice's token
        IAccount aliceAccount = accounts.stream()
                .filter(a -> aliceUpn.equalsIgnoreCase(a.username()))
                .findFirst().orElse(null);
        assertNotNull(aliceAccount, "Alice account should be in cache");
        IAuthenticationResult silentAlice = cca.acquireTokenSilently(
                SilentParameters.builder(SCOPES, aliceAccount).build()).get();
        assertEquals(aliceToken, silentAlice.accessToken(), "Silent for Alice should return Alice's token");

        // Silent for Bob → should return Bob's token
        IAccount bobAccount = accounts.stream()
                .filter(a -> bobUpn.equalsIgnoreCase(a.username()))
                .findFirst().orElse(null);
        assertNotNull(bobAccount, "Bob account should be in cache");
        IAuthenticationResult silentBob = cca.acquireTokenSilently(
                SilentParameters.builder(SCOPES, bobAccount).build()).get();
        assertEquals(bobToken, silentBob.accessToken(), "Silent for Bob should return Bob's token");
    }

    /**
     * Verifies that two different users (by OID) acquire tokens via user_fic on the same CCA,
     * and AcquireTokenSilent resolves the correct account by OID.
     */
    @Test
    void userFic_TwoOids_SilentReturnsCorrectToken() throws Exception {
        // Arrange
        String carolOid = "oid-carol-3333";
        String carolUpn = "carol@contoso.com";
        String carolToken = "access-token-carol";

        String daveOid = "oid-dave-4444";
        String daveUpn = "dave@contoso.com";
        String daveToken = "access-token-dave";

        String tid = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";

        AtomicReference<HttpResponse> nextResponse = new AtomicReference<>();

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> nextResponse.get());

        // Act: Acquire token for Carol (using UPN)
        nextResponse.set(createUserResponse(carolOid, carolUpn, carolToken, tid));

        UserFederatedIdentityCredentialParameters carolParams = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, carolUpn, FAKE_ASSERTION)
                .build();
        IAuthenticationResult carolResult = cca.acquireToken(carolParams).get();
        assertEquals(carolToken, carolResult.accessToken());

        // Act: Acquire token for Dave (using UPN)
        nextResponse.set(createUserResponse(daveOid, daveUpn, daveToken, tid));

        UserFederatedIdentityCredentialParameters daveParams = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, daveUpn, FAKE_ASSERTION)
                .build();
        IAuthenticationResult daveResult = cca.acquireToken(daveParams).get();
        assertEquals(daveToken, daveResult.accessToken());

        // Assert: Both accounts in cache
        Set<IAccount> accounts = cca.getAccounts().get();
        assertEquals(2, accounts.size(), "Two accounts should be cached");

        // Lookup by OID for Carol
        IAccount carolAccount = accounts.stream()
                .filter(a -> a.homeAccountId().contains(carolOid))
                .findFirst().orElse(null);
        assertNotNull(carolAccount, "Carol account should be in cache");
        IAuthenticationResult silentCarol = cca.acquireTokenSilently(
                SilentParameters.builder(SCOPES, carolAccount).build()).get();
        assertEquals(carolToken, silentCarol.accessToken(), "OID-based lookup for Carol should return Carol's token");

        // Lookup by OID for Dave
        IAccount daveAccount = accounts.stream()
                .filter(a -> a.homeAccountId().contains(daveOid))
                .findFirst().orElse(null);
        assertNotNull(daveAccount, "Dave account should be in cache");
        IAuthenticationResult silentDave = cca.acquireTokenSilently(
                SilentParameters.builder(SCOPES, daveAccount).build()).get();
        assertEquals(daveToken, silentDave.accessToken(), "OID-based lookup for Dave should return Dave's token");
    }

    /**
     * Verifies that the UUID builder overload combined with findCachedAccount()'s OID-based
     * matching produces a cache hit on the second call (no second network request).
     */
    @Test
    void userFic_UuidOverload_CacheHitOnSecondCall() throws Exception {
        // Arrange
        UUID userOid = UUID.fromString("11111111-2222-3333-4444-555555555555");
        String oidStr = userOid.toString();
        String upn = "eve@contoso.com";
        String token = "access-token-eve";
        String tid = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";

        AtomicInteger networkCalls = new AtomicInteger(0);

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            networkCalls.incrementAndGet();
            return createUserResponse(oidStr, upn, token, tid);
        });

        // Act 1: First call — goes to IdP
        UserFederatedIdentityCredentialParameters params = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, userOid, FAKE_ASSERTION)
                .build();
        IAuthenticationResult result1 = cca.acquireToken(params).get();
        assertEquals(token, result1.accessToken());
        assertEquals(1, networkCalls.get(), "First call should hit the network");

        // Act 2: Second call with same UUID and forceRefresh=false — should use cache
        UserFederatedIdentityCredentialParameters params2 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, userOid, FAKE_ASSERTION)
                .forceRefresh(false)
                .build();
        IAuthenticationResult result2 = cca.acquireToken(params2).get();
        assertEquals(token, result2.accessToken());
        assertEquals(1, networkCalls.get(), "Second call should NOT hit the network (cache hit via OID match)");
    }

    /**
     * Regression test: verifies that when only one user's token is cached, a request
     * for a different user does NOT silently return the first user's cached token.
     *
     * Before the fix, findCachedAccount() had a fallback: if accounts.size() == 1 it
     * returned that sole account regardless of UPN/OID match. This caused Bob's initial
     * acquireToken call to silently return Alice's cached token instead of going to the IdP.
     */
    @Test
    void userFic_SingleAccountFallback_DoesNotReturnWrongUserToken() throws Exception {
        // Arrange
        String aliceOid = "oid-alice-1111";
        String aliceUpn = "alice@contoso.com";
        String aliceToken = "access-token-alice";

        String bobOid = "oid-bob-2222";
        String bobUpn = "bob@contoso.com";
        String bobToken = "access-token-bob";

        String tid = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";

        AtomicInteger networkCalls = new AtomicInteger(0);
        AtomicReference<HttpResponse> nextResponse = new AtomicReference<>();

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            networkCalls.incrementAndGet();
            return nextResponse.get();
        });

        // Act 1: Acquire token for Alice (goes to IdP, gets cached)
        nextResponse.set(createUserResponse(aliceOid, aliceUpn, aliceToken, tid));

        UserFederatedIdentityCredentialParameters aliceParams = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, aliceUpn, FAKE_ASSERTION)
                .build();
        IAuthenticationResult aliceResult = cca.acquireToken(aliceParams).get();
        assertEquals(aliceToken, aliceResult.accessToken());
        int callsAfterAlice = networkCalls.get();

        // At this point, cache has exactly one account (Alice).
        assertEquals(1, cca.getAccounts().get().size(), "Only Alice should be in cache");

        // Act 2: Acquire token for Bob WITHOUT forceRefresh.
        // findCachedAccount() runs; there is one account (Alice) but it doesn't match Bob.
        // The correct behavior is to return null → go to IdP → get Bob's token.
        // The buggy behavior was: accounts.size()==1 → return Alice → return Alice's cached token.
        nextResponse.set(createUserResponse(bobOid, bobUpn, bobToken, tid));

        UserFederatedIdentityCredentialParameters bobParams = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, bobUpn, FAKE_ASSERTION)
                .build();
        IAuthenticationResult bobResult = cca.acquireToken(bobParams).get();

        // Assert: Bob should have gotten his own token, not Alice's
        assertEquals(bobToken, bobResult.accessToken(),
                "Bob should receive his own token, not Alice's cached token");
        assertNotEquals(aliceToken, bobResult.accessToken(),
                "Bob's token must differ from Alice's");

        // Assert: A network call was made for Bob (not just a cache hit)
        assertTrue(networkCalls.get() > callsAfterAlice,
                "A network call should have been made for Bob since Alice is a different user");

        // Assert: Both accounts now in cache
        assertEquals(2, cca.getAccounts().get().size(), "Both Alice and Bob should be in cache");
    }
}
