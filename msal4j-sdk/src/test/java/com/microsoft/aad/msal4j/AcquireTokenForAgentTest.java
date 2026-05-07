// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    void acquireTokenForAgent_sameAgentId_reusesCca() throws Exception {
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

    // ========================================================================
    // Cache isolation: two blueprint CCAs do not share agent CCA caches
    // ========================================================================

    @Test
    void acquireTokenForAgent_twoBlueprintCcas_noCacheBleed() throws Exception {
        // Arrange — two separate blueprint CCAs
        DefaultHttpClient httpClient1 = mock(DefaultHttpClient.class);
        when(httpClient1.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token-1"),
                        createAppTokenResponse("assertion-token-1"),
                        createUserTokenResponse("alice-token-1", USER1_UPN, USER1_OID));

        DefaultHttpClient httpClient2 = mock(DefaultHttpClient.class);
        when(httpClient2.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token-2"),
                        createAppTokenResponse("assertion-token-2"),
                        createUserTokenResponse("bob-token-2", USER2_UPN, USER2_OID));

        ConfidentialClientApplication blueprint1 = createBlueprintCca(httpClient1);
        ConfidentialClientApplication blueprint2 = createBlueprintCca(httpClient2);

        AgentIdentity aliceAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);
        AgentIdentity bobAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER2_UPN);

        // Act: acquire via blueprint1
        IAuthenticationResult result1 = blueprint1.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();
        assertEquals("alice-token-1", result1.accessToken());

        // Act: acquire via blueprint2
        IAuthenticationResult result2 = blueprint2.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, bobAgent).build()
        ).get();
        assertEquals("bob-token-2", result2.accessToken());

        // Assert: each blueprint has its own agent CCA cache
        assertEquals(1, blueprint1.agentCcaCache.size());
        assertEquals(1, blueprint2.agentCcaCache.size());

        // Blueprint1's agent CCA should only have Alice's token (not Bob's)
        // Verify blueprint1 still returns Alice from cache (no bleed from blueprint2)
        IAuthenticationResult result1Again = blueprint1.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();
        assertEquals("alice-token-1", result1Again.accessToken());
        verify(httpClient1, times(3)).send(any(HttpRequest.class)); // no new calls
    }

    // ========================================================================
    // UPN → OID shared cache: same user found by either identifier
    // ========================================================================

    @Test
    void acquireTokenForAgent_upnThenOid_sharesCache() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // First call (UPN): 3 HTTP calls (Legs 1+2+3)
        // Second call (OID for same user): should come from cache (0 HTTP calls)
        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);

        AgentIdentity aliceByUpn = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);
        UUID aliceOid = UUID.fromString(USER1_OID);
        AgentIdentity aliceByOid = new AgentIdentity(AGENT_APP_ID, aliceOid);

        // Act 1: acquire by UPN
        IAuthenticationResult upnResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceByUpn).build()
        ).get();
        assertEquals("alice-token", upnResult.accessToken());
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));

        // Act 2: acquire by OID for the same user — should hit cache
        IAuthenticationResult oidResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceByOid).build()
        ).get();
        assertEquals("alice-token", oidResult.accessToken());
        // No new HTTP calls — found via OID match in findMatchingAccount
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));
    }

    // ========================================================================
    // Parameter propagation: tenant override flows to inner calls
    // ========================================================================

    @Test
    void acquireTokenForAgent_withTenant_propagatesToInnerCalls() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        String overrideTenant = "override-tenant-id";

        // Act
        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, agent)
                        .tenant(overrideTenant)
                        .build()
        ).get();

        // Assert — at least one inner HTTP call should target the override tenant
        verify(httpClientMock, atLeastOnce()).send(argThat(request -> {
            String url = request.url() != null ? request.url().toString() : "";
            return url.contains(overrideTenant);
        }));
    }

    // ========================================================================
    // Parameter propagation: extra query parameters flow to inner calls
    // ========================================================================

    @Test
    void acquireTokenForAgent_withExtraQueryParams_propagatesToInnerCalls() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        Map<String, String> extraParams = new HashMap<>();
        extraParams.put("custom_param", "custom_value");

        // Act
        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, agent)
                        .extraQueryParameters(extraParams)
                        .build()
        ).get();

        // Assert — at least one inner HTTP call should include the extra query parameter
        verify(httpClientMock, atLeastOnce()).send(argThat(request -> {
            String body = request.body();
            return body != null && body.contains("custom_param=custom_value");
        }));
    }

    // ========================================================================
    // Parameter propagation: claims flow to inner calls
    // ========================================================================

    @Test
    void acquireTokenForAgent_withClaims_propagatesToInnerCalls() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createAppTokenResponse("fmi-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-token", USER1_UPN, USER1_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);
        AgentIdentity agent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);

        ClaimsRequest claims = new ClaimsRequest();
        claims.requestClaimInAccessToken("xms_cc", null);

        // Act
        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, agent)
                        .claims(claims)
                        .build()
        ).get();

        // Assert — at least one inner HTTP call should include the claims parameter
        verify(httpClientMock, atLeastOnce()).send(argThat(request -> {
            String body = request.body();
            return body != null && body.contains("claims=");
        }));
    }

    // ========================================================================
    // Comprehensive cache behavior test: verifies token counts, isolation
    // between users, isolation between agent and non-agent flows, and
    // correct silent lookups across all scenarios.
    // ========================================================================

    @Test
    void acquireTokenForAgent_comprehensiveCacheBehavior() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // Queue responses in order:
        // --- Agent flow for Alice (Legs 1+2+3 = 3 HTTP calls) ---
        // 1. Leg 1: FMI credential (blueprint app token)
        // 2. Leg 2: assertion token (agent CCA app token)
        // 3. Leg 3: user token for Alice
        // --- Agent flow for Bob (Leg 3 only = 1 HTTP call, Legs 1+2 cached) ---
        // 4. Leg 3: user token for Bob
        // --- Non-agent client_credentials for Charlie on BLUEPRINT CCA (1 HTTP call) ---
        // 5. App token for Charlie scopes on the blueprint
        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        // Alice (Legs 1+2+3)
                        createAppTokenResponse("fmi-credential-token"),
                        createAppTokenResponse("assertion-token"),
                        createUserTokenResponse("alice-agent-token", USER1_UPN, USER1_OID),
                        // Bob (Leg 3 only)
                        createUserTokenResponse("bob-agent-token", USER2_UPN, USER2_OID),
                        // Charlie (non-agent client_credentials on blueprint)
                        createAppTokenResponse("charlie-app-token"));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);

        AgentIdentity aliceAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);
        AgentIdentity bobAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER2_UPN);

        // ---- Step 1: Agent flow for Alice ----
        IAuthenticationResult aliceResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();

        assertEquals("alice-agent-token", aliceResult.accessToken());
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));

        // ---- Step 2: Agent flow for Bob (Legs 1+2 should be cached) ----
        IAuthenticationResult bobResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, bobAgent).build()
        ).get();

        assertEquals("bob-agent-token", bobResult.accessToken());
        verify(httpClientMock, times(4)).send(any(HttpRequest.class)); // +1 for Leg 3 only

        // ---- Step 3: Non-agent client_credentials on the BLUEPRINT CCA ----
        // This uses the blueprint's own token cache, NOT the agent CCA's cache.
        IAuthenticationResult charlieResult = blueprintCca.acquireToken(
                ClientCredentialParameters.builder(CALLER_SCOPES).build()
        ).get();

        assertEquals("charlie-app-token", charlieResult.accessToken());
        verify(httpClientMock, times(5)).send(any(HttpRequest.class));

        // ---- Cache state verification ----

        // Blueprint CCA's token cache:
        //   - 1 FMI credential (Leg 1, with extCacheKeyHash for fmi_path)
        //   - 1 non-agent app token (Charlie's client_credentials)
        // Total: 2 access tokens in the blueprint's own cache
        assertEquals(2, blueprintCca.tokenCache.accessTokens.size(),
                "Blueprint cache should have 2 tokens: FMI credential + non-agent app token");

        // Agent CCA cache should have exactly one entry (for AGENT_APP_ID)
        assertEquals(1, blueprintCca.agentCcaCache.size(),
                "Blueprint should have 1 agent CCA cached");

        ConfidentialClientApplication agentCca =
                blueprintCca.agentCcaCache.get("agent_" + AGENT_APP_ID);
        assertNotNull(agentCca, "Agent CCA should exist in cache");

        // Agent CCA's token cache:
        //   - 1 assertion token (Leg 2, app-level, scope=api://AzureADTokenExchange/.default)
        //   - 2 user tokens (Alice + Bob, scope=graph.microsoft.com/.default)
        // Total: 3 access tokens in the agent CCA's cache
        assertEquals(3, agentCca.tokenCache.accessTokens.size(),
                "Agent CCA cache should have 3 tokens: 1 assertion + 2 user tokens");

        // ---- Step 4: Silent retrieval for Alice (agent flow, should hit cache) ----
        IAuthenticationResult aliceSilent = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceAgent).build()
        ).get();

        assertEquals("alice-agent-token", aliceSilent.accessToken());
        verify(httpClientMock, times(5)).send(any(HttpRequest.class)); // still 5, no new calls

        // ---- Step 5: Silent retrieval for Bob (agent flow, should hit cache) ----
        IAuthenticationResult bobSilent = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, bobAgent).build()
        ).get();

        assertEquals("bob-agent-token", bobSilent.accessToken());
        verify(httpClientMock, times(5)).send(any(HttpRequest.class)); // still 5

        // ---- Step 6: Non-agent call again (should hit blueprint's cache) ----
        IAuthenticationResult charlieAgain = blueprintCca.acquireToken(
                ClientCredentialParameters.builder(CALLER_SCOPES).build()
        ).get();

        assertEquals("charlie-app-token", charlieAgain.accessToken());
        verify(httpClientMock, times(5)).send(any(HttpRequest.class)); // still 5

        // ---- Step 7: Verify Alice by OID also hits cache (UPN→OID shared cache) ----
        UUID aliceOid = UUID.fromString(USER1_OID);
        AgentIdentity aliceByOid = new AgentIdentity(AGENT_APP_ID, aliceOid);

        IAuthenticationResult aliceByOidResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(CALLER_SCOPES, aliceByOid).build()
        ).get();

        assertEquals("alice-agent-token", aliceByOidResult.accessToken());
        verify(httpClientMock, times(5)).send(any(HttpRequest.class)); // still 5

        // ---- Step 8: Verify cache counts haven't changed after all silent calls ----
        assertEquals(2, blueprintCca.tokenCache.accessTokens.size(),
                "Blueprint cache should still have 2 tokens after silent calls");
        assertEquals(3, agentCca.tokenCache.accessTokens.size(),
                "Agent CCA cache should still have 3 tokens after silent calls");

        // ---- Step 9: Verify cache key isolation between FMI and non-FMI tokens ----
        // The blueprint cache has tokens with different cache key structures:
        //   - FMI token has extCacheKeyHash (credential_type=AccessToken_Extended)
        //   - Non-agent token has no extCacheKeyHash (credential_type=AccessToken)
        boolean hasFmiToken = blueprintCca.tokenCache.accessTokens.values().stream()
                .anyMatch(at -> !StringHelper.isBlank(at.extCacheKeyHash()));
        boolean hasNonFmiToken = blueprintCca.tokenCache.accessTokens.values().stream()
                .anyMatch(at -> StringHelper.isBlank(at.extCacheKeyHash()));

        assertTrue(hasFmiToken, "Blueprint cache should contain an FMI token with extCacheKeyHash");
        assertTrue(hasNonFmiToken, "Blueprint cache should contain a non-FMI token without extCacheKeyHash");

        // ---- Step 10: Verify agent CCA user tokens have distinct homeAccountIds ----
        long distinctHomeAccountIds = agentCca.tokenCache.accessTokens.values().stream()
                .map(at -> at.homeAccountId)
                .filter(id -> id != null && !id.isEmpty())
                .distinct()
                .count();
        assertEquals(2, distinctHomeAccountIds,
                "Agent CCA should have 2 distinct homeAccountIds (Alice + Bob)");
    }

    // ========================================================================
    // Leg 2 cache key isolation: verifies that credential_fmi_path produces an
    // extCacheKeyHash on the assertion token, preventing collisions with user
    // tokens that share the same scope. Also verifies credential_fmi_path is
    // NOT sent in the HTTP request body (cache-key-only).
    // ========================================================================

    @Test
    void acquireTokenForAgent_leg2CacheIsolation_credentialFmiPathPreventsCollision() throws Exception {
        // Both the caller and Leg 2 use the same scope (api://AzureADTokenExchange/.default).
        // Without credential_fmi_path isolation, Leg 2's cache lookup could return Alice's
        // user token instead of the assertion token — an order-dependent collision.
        Set<String> exchangeScope = Collections.singleton("api://AzureADTokenExchange/.default");

        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        // Alice: Legs 1+2+3 (3 HTTP calls)
                        createAppTokenResponse("fmi-credential"),
                        createAppTokenResponse("correct-assertion-token"),
                        createUserTokenResponse("alice-user-token", USER1_UPN, USER1_OID),
                        // Bob: Leg 3 only (1 HTTP call — Legs 1+2 cached)
                        createUserTokenResponse("bob-user-token", USER2_UPN, USER2_OID));

        ConfidentialClientApplication blueprintCca = createBlueprintCca(httpClientMock);

        AgentIdentity aliceAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER1_UPN);
        AgentIdentity bobAgent = AgentIdentity.withUsername(AGENT_APP_ID, USER2_UPN);

        // ---- Step 1: Alice's full agent flow with exchange scope ----
        IAuthenticationResult aliceResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(exchangeScope, aliceAgent).build()
        ).get();

        assertEquals("alice-user-token", aliceResult.accessToken());
        verify(httpClientMock, times(3)).send(any(HttpRequest.class));

        // ---- Step 2: Verify Leg 2 token has credential_fmi_path cache isolation ----
        ConfidentialClientApplication agentCca =
                blueprintCca.agentCcaCache.get("agent_" + AGENT_APP_ID);
        assertNotNull(agentCca);

        // Agent CCA has 2 tokens: Leg 2 app token + Alice's user token, both with exchange scope
        assertEquals(2, agentCca.tokenCache.accessTokens.size());

        // Compute the expected hash for credential_fmi_path = agentAppId
        java.util.TreeMap<String, String> expectedComponents = new java.util.TreeMap<>();
        expectedComponents.put("credential_fmi_path", AGENT_APP_ID);
        String expectedHash = StringHelper.computeExtCacheKeyHash(expectedComponents);

        // The Leg 2 token (app-level, empty homeAccountId) must have the correct extCacheKeyHash
        AccessTokenCacheEntity leg2Token = agentCca.tokenCache.accessTokens.values().stream()
                .filter(at -> StringHelper.isBlank(at.homeAccountId) || at.homeAccountId.isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Leg 2 app token not found"));

        assertEquals(expectedHash, leg2Token.extCacheKeyHash(),
                "Leg 2 token should have extCacheKeyHash from credential_fmi_path");

        // Alice's user token must NOT have an extCacheKeyHash
        AccessTokenCacheEntity aliceToken = agentCca.tokenCache.accessTokens.values().stream()
                .filter(at -> !StringHelper.isBlank(at.homeAccountId) && !at.homeAccountId.isEmpty())
                .findFirst()
                .orElseThrow(() -> new AssertionError("Alice's user token not found"));

        assertTrue(StringHelper.isBlank(aliceToken.extCacheKeyHash()),
                "Alice's user token should NOT have an extCacheKeyHash");

        // ---- Step 3: Verify credential_fmi_path is NOT sent in any HTTP body ----
        // (It's cache-key-only, not a wire parameter)
        org.mockito.ArgumentCaptor<HttpRequest> requestCaptor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClientMock, times(3)).send(requestCaptor.capture());

        for (HttpRequest req : requestCaptor.getAllValues()) {
            String body = req.body() != null ? req.body() : "";
            assertFalse(body.contains("credential_fmi_path"),
                    "credential_fmi_path should NOT appear in any HTTP request body");
        }

        // ---- Step 4: Bob's agent flow — Leg 2 from cache, Leg 3 fresh ----
        // Clear invocations to count only Bob's HTTP calls
        clearInvocations(httpClientMock);

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(
                        createUserTokenResponse("bob-user-token", USER2_UPN, USER2_OID));

        IAuthenticationResult bobResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(exchangeScope, bobAgent).build()
        ).get();

        assertEquals("bob-user-token", bobResult.accessToken());

        // Only 1 HTTP call: Leg 3 for Bob. If Leg 2 had a cache collision, it would
        // miss the cache (wrong extCacheKeyHash) and make an extra network call.
        org.mockito.ArgumentCaptor<HttpRequest> bobCaptor = org.mockito.ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClientMock, times(1)).send(bobCaptor.capture());

        // Verify Bob's Leg 3 used the correct assertion (not Alice's user token)
        HttpRequest bobLeg3Request = bobCaptor.getValue();
        String bobBody = bobLeg3Request.body() != null ? bobLeg3Request.body() : "";
        assertTrue(bobBody.contains("correct-assertion-token"),
                "Bob's Leg 3 should use the cached Leg 2 assertion token as client_assertion");
        assertFalse(bobBody.contains("alice-user-token"),
                "Bob's Leg 3 should NOT use Alice's user token as the assertion");

        // ---- Step 5: Final cache state ----
        assertEquals(3, agentCca.tokenCache.accessTokens.size(),
                "Agent CCA should have 3 tokens: 1 Leg 2 assertion + 2 user tokens");
    }
}
