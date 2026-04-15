// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the composite AcquireTokenForAgent flow (§10 from AgentIDs_ComponentsReference).
 * Validates the three-leg orchestration, internal CCA caching, per-user token isolation,
 * and silent retrieval.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcquireTokenForAgentTest {

    private static final String BLUEPRINT_CLIENT_ID = "blueprint-client-id";
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant/";
    private static final Set<String> CALLER_SCOPES = Collections.singleton("https://graph.microsoft.com/.default");

    private static final String AGENT_APP_ID = "agent-app-id-abc";
    private static final String TENANT_ID = "f645ad92-e38d-4d1a-b510-d1b09a74a8ca";

    private static final String USER1_UPN = "alice@contoso.com";
    private static final String USER1_OID = "11111111-1111-1111-1111-111111111111";
    private static final String USER2_UPN = "bob@contoso.com";
    private static final String USER2_OID = "22222222-2222-2222-2222-222222222222";

    private ConfidentialClientApplication createBlueprintCca(DefaultHttpClient httpClientMock) throws Exception {
        return ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID,
                        ClientCredentialFactory.createFromSecret("secret"))
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(httpClientMock)
                .build();
    }

    /**
     * Creates a simple app-token response (for Leg 1 FMI credential or Leg 2 assertion token).
     */
    private HttpResponse createAppTokenResponse(String accessToken) {
        HashMap<String, String> responseValues = new HashMap<>();
        responseValues.put("access_token", accessToken);
        return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                TestHelper.getSuccessfulTokenResponse(responseValues));
    }

    /**
     * Creates a user-token response (for Leg 3) with id_token and client_info so that
     * an account is properly stored in the cache.
     */
    private HttpResponse createUserTokenResponse(String accessToken, String upn, String oid) {
        HashMap<String, String> idTokenValues = new HashMap<>();
        idTokenValues.put("oid", oid);
        idTokenValues.put("preferred_username", upn);
        String idToken = TestHelper.createIdToken(idTokenValues);

        String clientInfo = createClientInfo(oid);

        HashMap<String, String> responseValues = new HashMap<>();
        responseValues.put("access_token", accessToken);
        responseValues.put("id_token", idToken);
        responseValues.put("client_info", clientInfo);
        return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                TestHelper.getSuccessfulTokenResponse(responseValues));
    }

    private String createClientInfo(String uid) {
        String json = String.format("{\"uid\":\"%s\",\"utid\":\"%s\"}", uid, TENANT_ID);
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    // ========================================================================
    // Core composite test: two users + caching (matches .NET TwoUpns test)
    // ========================================================================

    @Test
    void acquireTokenForAgent_twoUpns_cacheReturnsCorrectUserToken() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // Queue 4 HTTP responses in order:
        // 1. Leg 1 (FMI credential for blueprint)
        // 2. Leg 2 (assertion token for agent CCA)
        // 3. Leg 3 (user token for alice)
        // 4. Leg 3 (user token for bob — Legs 1+2 are cached)
        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-credential-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-access-token", USER1_UPN, USER1_OID),
                        createUserTokenResponse("bob-access-token", USER2_UPN, USER2_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);

        AgentIdentity aliceAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);
        AgentIdentity bobAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER2_UPN);

        // Act 1: Alice — should trigger 3 HTTP calls (Leg 1 + Leg 2 + Leg 3)
        IAuthenticationResult result1 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();

        assertEquals("alice-access-token", result1.accessToken());
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));

        // Act 2: Bob — should trigger only 1 HTTP call (Leg 3; Legs 1+2 cached)
        IAuthenticationResult result2 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, bobAgent).build()
        ).get();

        assertEquals("bob-access-token", result2.accessToken());
        verify(httpClientMock, times(4)).send(any(HttpRequest.class));

        // Act 3: Alice again — should return from cache (0 HTTP calls)
        IAuthenticationResult result3 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();

        assertEquals("alice-access-token", result3.accessToken());
        verify(httpClientMock, times(4)).send(any(HttpRequest.class)); // still 4 total
    }

    // ========================================================================
    // App-only flow
    // ========================================================================

    @Test
    void acquireTokenForAgent_appOnly_acquiresTokenWithoutUserLeg() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // App-only: Leg 1 (FMI credential) + Leg 2 (app token for caller scopes)
        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-credential-token"),
                        createAppTokenResponse("agent-app-token"));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);

        AgentIdentity appOnlyAgent = AgentIdentity.appOnly(AGENT_APP_ID);

        // Act
        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, appOnlyAgent).build()
        ).get();

        // Assert
        assertEquals("agent-app-token", result.accessToken());
        verify(httpClientMock, times(2)).send(any(HttpRequest.class));
    }

    @Test
    void acquireTokenForAgent_appOnly_secondCallReturnsCached() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-credential-token"),
                        createAppTokenResponse("agent-app-token"));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity appOnlyAgent = AgentIdentity.appOnly(AGENT_APP_ID);

        // Act 1: first call triggers HTTP
        blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, appOnlyAgent).build()
        ).get();
        verify(httpClientMock, times(2)).send(any(HttpRequest.class));

        // Act 2: second call should return from app cache (0 new HTTP calls)
        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, appOnlyAgent).build()
        ).get();

        assertEquals("agent-app-token", result.accessToken());
        verify(httpClientMock, times(2)).send(any(HttpRequest.class)); // still 2 total
    }

    // ========================================================================
    // ForceRefresh bypasses user cache
    // ========================================================================

    @Test
    void acquireTokenForAgent_forceRefresh_bypassesUserCache() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // First call: 3 HTTP calls (Legs 1+2+3)
        // Second call with forceRefresh: Leg 3 again (Legs 1+2 still cached)
        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-credential-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-token-1", USER1_UPN, USER1_OID),
                        createUserTokenResponse("alice-token-2", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity aliceAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        // Act 1: normal call
        IAuthenticationResult result1 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();
        assertEquals("alice-token-1", result1.accessToken());
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));

        // Act 2: forceRefresh — should bypass user cache and execute Leg 3 again
        IAuthenticationResult result2 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent)
                        .forceRefresh(true)
                        .build()
        ).get();
        assertEquals("alice-token-2", result2.accessToken());
        // Leg 3 fires again (1 more HTTP call), Legs 1+2 still from cache
        verify(httpClientMock, times(4)).send(any(HttpRequest.class));
    }

    // ========================================================================
    // User identity by OID
    // ========================================================================

    @Test
    void acquireTokenForAgent_withOid_acquiresUserToken() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-credential-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);

        UUID aliceOid = UUID.fromString(USER1_OID);
        AgentIdentity aliceByOid = new AgentIdentity(AGENT_APP_ID, aliceOid);

        // Act
        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceByOid).build()
        ).get();

        // Assert
        assertEquals("alice-token", result.accessToken());
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));
    }

    // ========================================================================
    // Agent CCA caching: same agent ID reuses CCA
    // ========================================================================

    @Test
    void acquireTokenForAgent_samAgentId_reusesCca() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("user-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        // Act
        blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, agent).build()
        ).get();

        // Assert — agent CCA cache should have exactly one entry
        assertEquals(1, blueprintCca.agentCcaCache.size());
        assertTrue(blueprintCca.agentCcaCache.containsKey("agent_" + AGENT_APP_ID));
    }

    // ========================================================================
    // Leg 1 sends fmi_path in body
    // ========================================================================

    @Test
    void acquireTokenForAgent_leg1_sendsFmiPathInBody() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("user-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        // Act
        blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, agent).build()
        ).get();

        // Assert — first HTTP call (Leg 1) should include fmi_path=agentAppId
        verify(httpClientMock, atLeastOnce()).send(argThat(request -> {
            String body = request.body();
            return body != null && body.contains("fmi_path=" + AGENT_APP_ID);
        }));
    }

    // ========================================================================
    // Leg 3 sends user_fic grant type
    // ========================================================================

    @Test
    void acquireTokenForAgent_leg3_sendsUserFicGrantType() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("user-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        // Act
        blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, agent).build()
        ).get();

        // Assert — one of the HTTP calls should contain grant_type=user_fic
        verify(httpClientMock, atLeastOnce()).send(argThat(request -> {
            String body = request.body();
            return body != null && body.contains("grant_type=user_fic");
        }));
    }

    // ========================================================================
    // Input validation
    // ========================================================================

    @Test
    void acquireTokenForAgent_nullParameters_throwsException() throws Exception {
        ConfidentialClientApplication cca = createBlueprintCca(mock(DefaultHttpClient.class));
        assertThrows(IllegalArgumentException.class, () ->
                cca.acquireTokenForAgent(null));
    }

    @Test
    void parameterBuilder_nullScopes_throwsException() {
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);
        assertThrows(IllegalArgumentException.class, () ->
                AcquireTokenForAgentParameters.builder(null, agent));
    }

    @Test
    void parameterBuilder_nullAgentIdentity_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, null));
    }
}
