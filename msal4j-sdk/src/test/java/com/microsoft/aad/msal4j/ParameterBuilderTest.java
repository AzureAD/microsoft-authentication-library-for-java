// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for all IAcquireTokenParameters builder classes.
 * Each section covers: required params build, optional params, validation, and special logic.
 */
class ParameterBuilderTest {

    private static final Set<String> SCOPES = Collections.singleton("User.Read");
    private static final String TENANT = "contoso.onmicrosoft.com";
    private static final Map<String, String> EXTRA_HEADERS = Collections.singletonMap("x-custom", "value");
    private static final Map<String, String> EXTRA_QUERY_PARAMS = Collections.singletonMap("param1", "value1");

    // ========== DeviceCodeFlowParameters ==========

    @Test
    void deviceCodeFlow_RequiredParams_GettersReturnExpected() {
        Consumer<DeviceCode> consumer = dc -> {};

        DeviceCodeFlowParameters params = DeviceCodeFlowParameters
                .builder(SCOPES, consumer)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertSame(consumer, params.deviceCodeConsumer());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
    }

    @Test
    void deviceCodeFlow_AllOptionalParams_GettersReturnExpected() {
        Consumer<DeviceCode> consumer = dc -> {};
        ClaimsRequest claims = new ClaimsRequest();

        DeviceCodeFlowParameters params = DeviceCodeFlowParameters
                .builder(SCOPES, consumer)
                .claims(claims)
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .build();

        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void deviceCodeFlow_NullScopes_Throws() {
        Consumer<DeviceCode> consumer = dc -> {};

        assertThrows(IllegalArgumentException.class, () ->
                DeviceCodeFlowParameters.builder(null, consumer));
    }

    @Test
    void deviceCodeFlow_DeviceCodeConsumerValidation_RejectsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                DeviceCodeFlowParameters
                        .builder(SCOPES, dc -> {})
                        .deviceCodeConsumer(null)
                        .build());
    }

    // ========== IntegratedWindowsAuthenticationParameters ==========

    @Test
    void iwa_RequiredParams_GettersReturnExpected() {
        IntegratedWindowsAuthenticationParameters params =
                IntegratedWindowsAuthenticationParameters
                        .builder(SCOPES, "user@contoso.com")
                        .build();

        assertEquals(SCOPES, params.scopes());
        assertEquals("user@contoso.com", params.username());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
    }

    @Test
    void iwa_AllOptionalParams_GettersReturnExpected() {
        ClaimsRequest claims = new ClaimsRequest();

        IntegratedWindowsAuthenticationParameters params =
                IntegratedWindowsAuthenticationParameters
                        .builder(SCOPES, "user@contoso.com")
                        .claims(claims)
                        .extraHttpHeaders(EXTRA_HEADERS)
                        .extraQueryParameters(EXTRA_QUERY_PARAMS)
                        .tenant(TENANT)
                        .build();

        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void iwa_NullScopes_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                IntegratedWindowsAuthenticationParameters.builder(null, "user@contoso.com"));
    }

    @Test
    void iwa_BlankUsername_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                IntegratedWindowsAuthenticationParameters.builder(SCOPES, ""));
    }

    // ========== AppTokenProviderParameters ==========

    @Test
    void appTokenProvider_Constructor_GettersReturnExpected() {
        Set<String> scopes = Collections.singleton("https://graph.microsoft.com/.default");
        String correlationId = "corr-123";
        String claims = "{\"access_token\":{}}";
        String tenantId = "tenant-456";

        AppTokenProviderParameters params = new AppTokenProviderParameters(
                scopes, correlationId, claims, tenantId);

        assertEquals(scopes, params.getScopes());
        assertEquals(correlationId, params.getCorrelationId());
        assertEquals(claims, params.getClaims());
        assertEquals(tenantId, params.getTenantId());
    }

    @Test
    void appTokenProvider_Setters_UpdateValues() {
        AppTokenProviderParameters params = new AppTokenProviderParameters(
                SCOPES, "corr-1", null, "tenant-1");

        Set<String> newScopes = Collections.singleton("Mail.Read");
        params.setScopes(newScopes);
        params.setCorrelationId("corr-2");
        params.setClaims("new-claims");
        params.setTenantId("tenant-2");

        assertEquals(newScopes, params.getScopes());
        assertEquals("corr-2", params.getCorrelationId());
        assertEquals("new-claims", params.getClaims());
        assertEquals("tenant-2", params.getTenantId());
    }

    // ========== PopParameters ==========

    @Test
    void pop_ValidParams_GettersReturnExpected() throws Exception {
        URI uri = new URI("https://graph.microsoft.com/v1.0/me");

        PopParameters params = new PopParameters(HttpMethod.GET, uri, "test-nonce");

        assertEquals(HttpMethod.GET, params.getHttpMethod());
        assertEquals(uri, params.getUri());
        assertEquals("test-nonce", params.getNonce());
    }

    @Test
    void pop_NullHttpMethod_Throws() throws Exception {
        URI uri = new URI("https://graph.microsoft.com/v1.0/me");

        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                new PopParameters(null, uri, "nonce"));
        assertTrue(ex.getMessage().contains("HTTP method"));
    }

    @Test
    void pop_NullUri_Throws() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                new PopParameters(HttpMethod.GET, null, "nonce"));
        assertTrue(ex.getMessage().contains("HTTP method"));
    }

    @Test
    void pop_UriWithNullHost_Throws() throws Exception {
        // A URI like "urn:example" has no host component
        URI noHostUri = new URI("urn:example");
        assertNull(noHostUri.getHost());

        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                new PopParameters(HttpMethod.GET, noHostUri, "nonce"));
        assertTrue(ex.getMessage().contains("HTTP method"));
    }

    @Test
    void pop_NullNonce_DoesNotThrow() throws Exception {
        URI uri = new URI("https://graph.microsoft.com/v1.0/me");

        PopParameters params = new PopParameters(HttpMethod.POST, uri, null);

        assertEquals(HttpMethod.POST, params.getHttpMethod());
        assertNull(params.getNonce());
    }

    // ========== InteractiveRequestParameters ==========

    @Test
    void interactive_RequiredParams_GettersReturnExpected() throws Exception {
        URI redirectUri = new URI("http://localhost:8080");

        InteractiveRequestParameters params = InteractiveRequestParameters
                .builder(redirectUri)
                .build();

        assertEquals(redirectUri, params.redirectUri());
        assertNull(params.scopes());
        assertNull(params.claims());
        assertNull(params.prompt());
        assertNull(params.loginHint());
        assertNull(params.domainHint());
        assertNull(params.systemBrowserOptions());
        assertNull(params.claimsChallenge());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
        assertEquals(120, params.httpPollingTimeoutInSeconds());
        assertFalse(params.instanceAware());
        assertEquals(0L, params.windowHandle());
        assertNull(params.proofOfPossession());
    }

    @Test
    void interactive_AllOptionalParams_GettersReturnExpected() throws Exception {
        URI redirectUri = new URI("http://localhost:8080");
        ClaimsRequest claims = new ClaimsRequest();
        SystemBrowserOptions browserOptions = SystemBrowserOptions.builder().build();

        InteractiveRequestParameters params = InteractiveRequestParameters
                .builder(redirectUri)
                .scopes(SCOPES)
                .claims(claims)
                .prompt(Prompt.CONSENT)
                .loginHint("user@contoso.com")
                .domainHint("contoso.com")
                .systemBrowserOptions(browserOptions)
                .claimsChallenge("{\"access_token\":{}}")
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .httpPollingTimeoutInSeconds(60)
                .instanceAware(true)
                .windowHandle(12345L)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertSame(claims, params.claims());
        assertEquals(Prompt.CONSENT, params.prompt());
        assertEquals("user@contoso.com", params.loginHint());
        assertEquals("contoso.com", params.domainHint());
        assertSame(browserOptions, params.systemBrowserOptions());
        assertEquals("{\"access_token\":{}}", params.claimsChallenge());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
        assertEquals(60, params.httpPollingTimeoutInSeconds());
        assertTrue(params.instanceAware());
        assertEquals(12345L, params.windowHandle());
    }

    @Test
    void interactive_NullRedirectUri_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                InteractiveRequestParameters.builder(null));
    }

    @Test
    void interactive_ProofOfPossession_CreatesPopParameters() throws Exception {
        URI redirectUri = new URI("http://localhost:8080");
        URI resourceUri = new URI("https://graph.microsoft.com/v1.0/me");

        InteractiveRequestParameters params = InteractiveRequestParameters
                .builder(redirectUri)
                .proofOfPossession(HttpMethod.GET, resourceUri, "nonce-value")
                .build();

        PopParameters pop = params.proofOfPossession();
        assertNotNull(pop);
        assertEquals(HttpMethod.GET, pop.getHttpMethod());
        assertEquals(resourceUri, pop.getUri());
        assertEquals("nonce-value", pop.getNonce());
    }

    // ========== SilentParameters ==========

    @Test
    void silent_WithAccount_GettersReturnExpected() {
        IAccount mockAccount = new IAccount() {
            public String homeAccountId() { return "uid.utid"; }
            public String environment() { return "login.microsoftonline.com"; }
            public String username() { return "user@contoso.com"; }
            public Map<String, ITenantProfile> getTenantProfiles() { return null; }
        };

        SilentParameters params = SilentParameters
                .builder(SCOPES, mockAccount)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertSame(mockAccount, params.account());
        assertFalse(params.forceRefresh());
        assertNull(params.claims());
        assertNull(params.authorityUrl());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
        assertNull(params.proofOfPossession());
    }

    @Test
    void silent_WithoutAccount_GettersReturnExpected() {
        SilentParameters params = SilentParameters
                .builder(SCOPES)
                .forceRefresh(true)
                .tenant(TENANT)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertNull(params.account());
        assertTrue(params.forceRefresh());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void silent_RemoveEmptyScope_FiltersEmptyStrings() {
        Set<String> scopesWithEmpty = new HashSet<>();
        scopesWithEmpty.add("User.Read");
        scopesWithEmpty.add("");
        scopesWithEmpty.add("Mail.Read");

        SilentParameters params = SilentParameters
                .builder(scopesWithEmpty)
                .build();

        Set<String> resultScopes = params.scopes();
        assertEquals(2, resultScopes.size());
        assertTrue(resultScopes.contains("User.Read"));
        assertTrue(resultScopes.contains("Mail.Read"));
        assertFalse(resultScopes.contains(""));
    }

    @Test
    void silent_NullScopes_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                SilentParameters.builder(null));
    }

    @Test
    void silent_NullAccount_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                SilentParameters.builder(SCOPES, null));
    }

    @Test
    void silent_ProofOfPossession_CreatesPopParameters() throws Exception {
        URI resourceUri = new URI("https://graph.microsoft.com/v1.0/me");

        SilentParameters params = SilentParameters
                .builder(SCOPES)
                .proofOfPossession(HttpMethod.POST, resourceUri, "nonce")
                .build();

        PopParameters pop = params.proofOfPossession();
        assertNotNull(pop);
        assertEquals(HttpMethod.POST, pop.getHttpMethod());
        assertEquals(resourceUri, pop.getUri());
    }

    // ========== OnBehalfOfParameters ==========

    @Test
    void obo_RequiredParams_GettersReturnExpected() {
        UserAssertion assertion = new UserAssertion("test-jwt-assertion");

        OnBehalfOfParameters params = OnBehalfOfParameters
                .builder(SCOPES, assertion)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertSame(assertion, params.userAssertion());
        assertFalse(params.skipCache());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
    }

    @Test
    void obo_AllOptionalParams_GettersReturnExpected() {
        UserAssertion assertion = new UserAssertion("test-jwt-assertion");
        ClaimsRequest claims = new ClaimsRequest();

        OnBehalfOfParameters params = OnBehalfOfParameters
                .builder(SCOPES, assertion)
                .skipCache(true)
                .claims(claims)
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .build();

        assertTrue(params.skipCache());
        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void obo_NullScopes_Throws() {
        UserAssertion assertion = new UserAssertion("test-jwt-assertion");

        assertThrows(IllegalArgumentException.class, () ->
                OnBehalfOfParameters.builder(null, assertion));
    }

    @Test
    void obo_SkipCacheNull_DefaultsToFalse() {
        UserAssertion assertion = new UserAssertion("test-jwt-assertion");

        OnBehalfOfParameters params = OnBehalfOfParameters
                .builder(SCOPES, assertion)
                .skipCache(null)
                .build();

        // OnBehalfOfParameters constructor: skipCache = skipCache != null && skipCache
        assertFalse(params.skipCache());
    }

    @Test
    void obo_SkipCacheTrue_ReturnsTrueValue() {
        UserAssertion assertion = new UserAssertion("test-jwt-assertion");

        OnBehalfOfParameters params = OnBehalfOfParameters
                .builder(SCOPES, assertion)
                .skipCache(Boolean.TRUE)
                .build();

        assertTrue(params.skipCache());
    }

    // ========== RefreshTokenParameters ==========

    @Test
    void refreshToken_RequiredParams_GettersReturnExpected() {
        RefreshTokenParameters params = RefreshTokenParameters
                .builder(SCOPES, "refresh-token-value")
                .build();

        assertEquals(SCOPES, params.scopes());
        assertEquals("refresh-token-value", params.refreshToken());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
    }

    @Test
    void refreshToken_AllOptionalParams_GettersReturnExpected() {
        ClaimsRequest claims = new ClaimsRequest();

        RefreshTokenParameters params = RefreshTokenParameters
                .builder(SCOPES, "refresh-token-value")
                .claims(claims)
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .build();

        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void refreshToken_BlankToken_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                RefreshTokenParameters.builder(SCOPES, ""));
    }

    @Test
    void refreshToken_BuilderRefreshTokenValidation_RejectsNull() {
        assertThrows(IllegalArgumentException.class, () ->
                RefreshTokenParameters
                        .builder(SCOPES, "initial-token")
                        .refreshToken(null)
                        .build());
    }

    // ========== AuthorizationCodeParameters ==========

    @Test
    void authCode_RequiredParams_GettersReturnExpected() throws Exception {
        URI redirectUri = new URI("http://localhost:8080");

        AuthorizationCodeParameters params = AuthorizationCodeParameters
                .builder("auth-code-123", redirectUri)
                .build();

        assertEquals("auth-code-123", params.authorizationCode());
        assertEquals(redirectUri, params.redirectUri());
        assertNull(params.scopes());
        assertNull(params.claims());
        assertNull(params.codeVerifier());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
    }

    @Test
    void authCode_AllOptionalParams_GettersReturnExpected() throws Exception {
        URI redirectUri = new URI("http://localhost:8080");
        ClaimsRequest claims = new ClaimsRequest();

        AuthorizationCodeParameters params = AuthorizationCodeParameters
                .builder("auth-code-123", redirectUri)
                .scopes(SCOPES)
                .claims(claims)
                .codeVerifier("pkce-verifier")
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertSame(claims, params.claims());
        assertEquals("pkce-verifier", params.codeVerifier());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void authCode_BlankCode_Throws() throws Exception {
        URI redirectUri = new URI("http://localhost:8080");

        assertThrows(IllegalArgumentException.class, () ->
                AuthorizationCodeParameters.builder("", redirectUri));
    }

    // ========== ClientCredentialParameters ==========

    @Test
    void clientCredential_RequiredParams_GettersReturnExpected() {
        ClientCredentialParameters params = ClientCredentialParameters
                .builder(SCOPES)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertFalse(params.skipCache());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
        assertNull(params.clientCredential());
        assertNull(params.fmiPath());
    }

    @Test
    void clientCredential_AllOptionalParams_GettersReturnExpected() {
        ClaimsRequest claims = new ClaimsRequest();
        IClientCredential credential = ClientCredentialFactory.createFromSecret("test-secret");

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(SCOPES)
                .skipCache(true)
                .claims(claims)
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .clientCredential(credential)
                .fmiPath("agent-app-id")
                .build();

        assertTrue(params.skipCache());
        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
        assertSame(credential, params.clientCredential());
        assertEquals("agent-app-id", params.fmiPath());
    }

    @Test
    void clientCredential_NullScopes_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ClientCredentialParameters.builder(null));
    }

    @Test
    void clientCredential_FmiPath_ComputesExtCacheKeyHash() {
        ClientCredentialParameters params = ClientCredentialParameters
                .builder(SCOPES)
                .fmiPath("agent-app-id")
                .build();

        // cacheKeyComponents should contain fmi_path
        assertNotNull(params.cacheKeyComponents());
        assertEquals("agent-app-id", params.cacheKeyComponents().get("fmi_path"));

        // computeExtCacheKeyHash should return a non-empty string
        String hash = params.computeExtCacheKeyHash();
        assertNotNull(hash);
        assertFalse(hash.isEmpty());

        // Second call should return memoized value (same instance)
        assertSame(hash, params.computeExtCacheKeyHash());
    }

    @Test
    void clientCredential_NoFmiPath_NullCacheKeyComponents() {
        ClientCredentialParameters params = ClientCredentialParameters
                .builder(SCOPES)
                .build();

        assertNull(params.cacheKeyComponents());
        assertEquals("", params.computeExtCacheKeyHash());
    }

    @Test
    void clientCredential_BlankFmiPath_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ClientCredentialParameters.builder(SCOPES).fmiPath(""));
    }

    // ========== UserNamePasswordParameters ==========

    @Test
    void usernamePassword_RequiredParams_GettersReturnExpected() {
        char[] password = "P@ssw0rd".toCharArray();

        UserNamePasswordParameters params = UserNamePasswordParameters
                .builder(SCOPES, "user@contoso.com", password)
                .build();

        assertEquals(SCOPES, params.scopes());
        assertEquals("user@contoso.com", params.username());
        assertArrayEquals(password, params.password());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
        assertNull(params.proofOfPossession());
    }

    @Test
    void usernamePassword_AllOptionalParams_GettersReturnExpected() throws Exception {
        ClaimsRequest claims = new ClaimsRequest();
        URI resourceUri = new URI("https://graph.microsoft.com/v1.0/me");

        UserNamePasswordParameters params = UserNamePasswordParameters
                .builder(SCOPES, "user@contoso.com", "P@ssw0rd".toCharArray())
                .claims(claims)
                .extraHttpHeaders(EXTRA_HEADERS)
                .extraQueryParameters(EXTRA_QUERY_PARAMS)
                .tenant(TENANT)
                .proofOfPossession(HttpMethod.GET, resourceUri, "nonce")
                .build();

        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
        assertNotNull(params.proofOfPossession());
    }

    @Test
    void usernamePassword_BlankUsername_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserNamePasswordParameters.builder(SCOPES, "", "pass".toCharArray()));
    }

    @Test
    void usernamePassword_EmptyPassword_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserNamePasswordParameters.builder(SCOPES, "user@contoso.com", new char[0]));
    }

    @Test
    void usernamePassword_PasswordCloned_NotSameArray() {
        char[] original = "P@ssw0rd".toCharArray();

        UserNamePasswordParameters params = UserNamePasswordParameters
                .builder(SCOPES, "user@contoso.com", original)
                .build();

        // password() returns a clone, not the same array
        char[] returned = params.password();
        assertArrayEquals(original, returned);
        assertNotSame(original, returned);
    }

    // ========== UserFederatedIdentityCredentialParameters ==========

    @Test
    void userFic_BuilderWithUsername_GettersReturnExpected() {
        UserFederatedIdentityCredentialParameters params =
                UserFederatedIdentityCredentialParameters
                        .builder(SCOPES, "user@contoso.com", "jwt-assertion")
                        .build();

        assertEquals(SCOPES, params.scopes());
        assertEquals("user@contoso.com", params.username());
        assertNull(params.userObjectId());
        assertEquals("jwt-assertion", params.assertion());
        assertFalse(params.forceRefresh());
        assertNull(params.claims());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertNull(params.tenant());
    }

    @Test
    void userFic_BuilderWithObjectId_GettersReturnExpected() {
        UUID objectId = UUID.randomUUID();

        UserFederatedIdentityCredentialParameters params =
                UserFederatedIdentityCredentialParameters
                        .builder(SCOPES, objectId, "jwt-assertion")
                        .build();

        assertEquals(SCOPES, params.scopes());
        assertNull(params.username());
        assertEquals(objectId, params.userObjectId());
        assertEquals("jwt-assertion", params.assertion());
    }

    @Test
    void userFic_AllOptionalParams_GettersReturnExpected() {
        ClaimsRequest claims = new ClaimsRequest();

        UserFederatedIdentityCredentialParameters params =
                UserFederatedIdentityCredentialParameters
                        .builder(SCOPES, "user@contoso.com", "jwt-assertion")
                        .forceRefresh(true)
                        .claims(claims)
                        .extraHttpHeaders(EXTRA_HEADERS)
                        .extraQueryParameters(EXTRA_QUERY_PARAMS)
                        .tenant(TENANT)
                        .build();

        assertTrue(params.forceRefresh());
        assertSame(claims, params.claims());
        assertEquals(EXTRA_HEADERS, params.extraHttpHeaders());
        assertEquals(EXTRA_QUERY_PARAMS, params.extraQueryParameters());
        assertEquals(TENANT, params.tenant());
    }

    @Test
    void userFic_NullScopes_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(null, "user@contoso.com", "assertion"));
    }

    @Test
    void userFic_BlankAssertion_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, "user@contoso.com", ""));
    }

    @Test
    void userFic_NullObjectId_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                UserFederatedIdentityCredentialParameters.builder(SCOPES, (UUID) null, "assertion"));
    }

    // ========== ManagedIdentityParameters ==========

    @Test
    void managedIdentity_RequiredParams_GettersReturnExpected() {
        ManagedIdentityParameters params = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .build();

        assertEquals("https://management.azure.com", params.resource());
        assertFalse(params.forceRefresh());
        assertNull(params.claims());
        assertNull(params.scopes());
        assertNull(params.extraHttpHeaders());
        assertNull(params.extraQueryParameters());
        assertEquals(Constants.MANAGED_IDENTITY_DEFAULT_TENTANT, params.tenant());
        assertNull(params.revokedTokenHash());
    }

    @Test
    void managedIdentity_ForceRefresh_SetCorrectly() {
        ManagedIdentityParameters params = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .forceRefresh(true)
                .build();

        assertTrue(params.forceRefresh());
    }

    @Test
    void managedIdentity_ValidClaims_ParsedAsClaimsRequest() {
        String claimsJson = "{\"access_token\":{\"nbf\":{\"essential\":true}}}";

        ManagedIdentityParameters params = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .claims(claimsJson)
                .build();

        ClaimsRequest parsedClaims = params.claims();
        assertNotNull(parsedClaims);
    }

    @Test
    void managedIdentity_NullClaims_ReturnsNull() {
        ManagedIdentityParameters params = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .build();

        assertNull(params.claims());
    }

    @Test
    void managedIdentity_EmptyClaims_Throws() {
        // The builder validates claims are not blank, so empty claims can only
        // happen if constructed directly. Testing the claims() method's own
        // empty-string guard (line 33: if claims == null || claims.isEmpty()).
        // Since builder prevents this, we verify the builder validation instead.
        assertThrows(IllegalArgumentException.class, () ->
                ManagedIdentityParameters.builder("https://management.azure.com").claims(""));
    }

    @Test
    void managedIdentity_BlankClaims_Throws() {
        assertThrows(IllegalArgumentException.class, () ->
                ManagedIdentityParameters.builder("https://management.azure.com").claims("   "));
    }

    // ========== ParameterValidationUtils ==========

    @Test
    void validateNotEmpty_set_nullThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ParameterValidationUtils.validateNotEmpty("scopes", (Set<String>) null));
    }

    @Test
    void validateNotEmpty_set_emptyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> ParameterValidationUtils.validateNotEmpty("scopes", new HashSet<>()));
    }

    @Test
    void validateNotEmpty_set_nonEmptyPasses() {
        ParameterValidationUtils.validateNotEmpty("scopes", Collections.singleton("openid"));
    }
}
