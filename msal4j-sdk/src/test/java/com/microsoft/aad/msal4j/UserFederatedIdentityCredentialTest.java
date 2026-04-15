// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the User Federated Identity Credential (user_fic) flow.
 * Covers §6 (user_fic grant type), §7 (user_federated_identity_credential body param),
 * §8 (user_id/username body params), and §11 (primitive API) from AgentIDs_ComponentsReference.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserFederatedIdentityCredentialTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant/";
    private static final Set<String> SCOPES = Collections.singleton("https://graph.microsoft.com/.default");
    private static final String FAKE_ASSERTION = "fake.assertion.jwt";
    private static final String TEST_UPN = "user@contoso.com";
    private static final UUID TEST_OID = UUID.fromString("597f86cd-13f3-44c0-bece-a1e77ba43228");

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
    // §6: user_fic grant type
    // ========================================================================

    @Test
    void userFic_SendsCorrectGrantType() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
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
    // §7: user_federated_identity_credential body parameter
    // ========================================================================

    @Test
    void userFic_SendsAssertionInBody() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
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
    // §8: user_id / username body parameters — mutual exclusion
    // ========================================================================

    @Test
    void userFic_WithUpn_SendsUsernameNotUserId() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
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
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_OID, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — user_id present, username absent (as grant param, may appear in common params)
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("user_id=" + TEST_OID.toString())
                    && !body.contains("username=");
        }));
    }

    // ========================================================================
    // §6+§7+§8 combined: all parameters sent together
    // ========================================================================

    @Test
    void userFic_SendsAllOAuthParametersTogether() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
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
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponse());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
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
    // §11: Token stored in user cache
    // ========================================================================

    @Test
    void userFic_TokenStoredInUserCache() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(createSuccessResponseWithIdToken());

        ConfidentialClientApplication cca = createCca(httpClientMock);

        UserFederatedIdentityCredentialParameters parameters = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();

        // Act
        IAuthenticationResult result = cca.acquireToken(parameters).get();

        // Assert — account is present (token stored in user cache)
        assertNotNull(result.account(), "Result should have an account (user token cache)");

        Set<IAccount> accounts = cca.getAccounts().get();
        assertFalse(accounts.isEmpty(), "Accounts should be present in the cache");
    }

    // ========================================================================
    // §11: Force refresh bypasses cache
    // ========================================================================

    @Test
    void userFic_ForceRefresh_BypassesCache() throws Exception {
        // Arrange
        AtomicInteger callCount = new AtomicInteger(0);

        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            callCount.incrementAndGet();
            return createSuccessResponseWithIdToken();
        });

        ConfidentialClientApplication cca = createCca(httpClientMock);

        // First call — populates cache
        UserFederatedIdentityCredentialParameters params1 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();
        cca.acquireToken(params1).get();
        assertEquals(1, callCount.get(), "First call should hit IdP");

        // Second call with forceRefresh — should bypass cache
        UserFederatedIdentityCredentialParameters params2 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();
        cca.acquireToken(params2).get();
        assertEquals(2, callCount.get(), "Second call with forceRefresh should hit IdP again");
    }

    // ========================================================================
    // §11: Cache hit when not force-refreshing
    // ========================================================================

    @Test
    void userFic_CacheHit_WhenNotForceRefreshing() throws Exception {
        // Arrange
        AtomicInteger callCount = new AtomicInteger(0);

        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            callCount.incrementAndGet();
            return createSuccessResponseWithIdToken();
        });

        ConfidentialClientApplication cca = createCca(httpClientMock);

        // First call — populates cache
        UserFederatedIdentityCredentialParameters params1 = UserFederatedIdentityCredentialParameters
                .builder(SCOPES, TEST_UPN, FAKE_ASSERTION)
                .forceRefresh(true)
                .build();
        cca.acquireToken(params1).get();
        assertEquals(1, callCount.get(), "First call should hit IdP");

        // Second call without forceRefresh — should use cache
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
}
