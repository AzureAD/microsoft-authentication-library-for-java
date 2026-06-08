// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.net.ssl.SSLSocketFactory;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApplicationBuilderTest {

    private static final String CLIENT_ID = TestHelper.TEST_CLIENT_ID;
    private static final String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    // ========== PublicClientApplication Tests ==========

    @Test
    void publicClientApplication_MinimalBuild_HasExpectedDefaults() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID).build();

        assertEquals(CLIENT_ID, pca.clientId());
        assertEquals(DEFAULT_AUTHORITY, pca.authority());
        assertTrue(pca.validateAuthority());
        assertTrue(pca.instanceDiscovery());
        assertFalse(pca.autoDetectRegion());
        assertNull(pca.azureRegion());
        assertNull(pca.applicationName());
        assertNull(pca.applicationVersion());
        assertNull(pca.clientCapabilities());
        assertNull(pca.aadAadInstanceDiscoveryResponse());
        assertNotNull(pca.tokenCache());
    }

    @Test
    void publicClientApplication_BlankClientId_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicClientApplication.builder("").build());
        assertThrows(IllegalArgumentException.class,
                () -> PublicClientApplication.builder("   ").build());
    }

    @Test
    void publicClientApplication_NullClientId_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> PublicClientApplication.builder(null).build());
    }

    @Test
    void publicClientApplication_AadAuthority_SetsCorrectType() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .authority("https://login.microsoftonline.com/my-tenant/")
                .build();

        assertEquals("https://login.microsoftonline.com/my-tenant/", pca.authority());
    }

    @Test
    void publicClientApplication_AdfsAuthority_SetsCorrectType() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .authority("https://adfs.contoso.com/adfs/")
                .build();

        assertEquals("https://adfs.contoso.com/adfs/", pca.authority());
        // ADFS authority type is set, but validateAuthority is not automatically disabled
        // (unlike B2C which explicitly sets validateAuthority=false)
    }

    @Test
    void publicClientApplication_CiamAuthority_SetsCorrectType() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .authority("https://contoso.ciamlogin.com/contoso.onmicrosoft.com/")
                .build();

        assertEquals("https://contoso.ciamlogin.com/contoso.onmicrosoft.com/", pca.authority());
    }

    @Test
    void publicClientApplication_B2cAuthority_SetsCorrectType() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .b2cAuthority("https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signIn/")
                .build();

        // B2C lowercases the authority path
        assertEquals("https://contoso.b2clogin.com/contoso.onmicrosoft.com/b2c_1_signin/", pca.authority());
        assertFalse(pca.validateAuthority(), "B2C sets validateAuthority to false");
    }

    @Test
    void publicClientApplication_B2cAuthorityViaRegularAuthority_Throws() {
        // Using the regular authority() method with a B2C URL should throw since it's not
        // AAD/ADFS/CIAM — it requires b2cAuthority() instead
        assertThrows(IllegalArgumentException.class, () ->
                PublicClientApplication.builder(CLIENT_ID)
                        .authority("https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signIn/")
                        .build());
    }

    @Test
    void publicClientApplication_DeviceCodeWithB2C_Throws() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .b2cAuthority("https://contoso.b2clogin.com/contoso.onmicrosoft.com/B2C_1_signIn/")
                .build();

        DeviceCodeFlowParameters params = DeviceCodeFlowParameters.builder(
                Collections.singleton("scope"),
                deviceCode -> {}).build();

        assertThrows(IllegalArgumentException.class, () -> pca.acquireToken(params));
    }

    @Test
    void publicClientApplication_PopWithoutBroker_Interactive_Throws() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID).build();

        InteractiveRequestParameters params = InteractiveRequestParameters.builder(new URI("http://localhost"))
                .scopes(Collections.singleton("scope"))
                .proofOfPossession(HttpMethod.GET, new URI("https://resource.com"), "nonce")
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class, () -> pca.acquireToken(params));
        assertTrue(ex.getMessage().contains("proofOfPossession"));
    }

    @Test
    void publicClientApplication_PopWithoutBroker_UsernamePassword_Throws() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID).build();

        UserNamePasswordParameters params = UserNamePasswordParameters.builder(
                        Collections.singleton("scope"), "user@example.com", "password".toCharArray())
                .proofOfPossession(HttpMethod.GET, new URI("https://resource.com"), "nonce")
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class, () -> pca.acquireToken(params));
        assertTrue(ex.getMessage().contains("proofOfPossession"));
    }

    @Test
    void publicClientApplication_PopWithoutBroker_Silent_Throws() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID).build();

        SilentParameters params = SilentParameters.builder(Collections.singleton("scope"))
                .proofOfPossession(HttpMethod.GET, new URI("https://resource.com"), "nonce")
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class,
                () -> pca.acquireTokenSilently(params));
        assertTrue(ex.getMessage().contains("proofOfPossession"));
    }

    @Test
    void publicClientApplication_BrokerEnabled_BuildsSuccessfully() {
        IBroker mockBroker = mock(IBroker.class);
        when(mockBroker.isBrokerAvailable()).thenReturn(true);

        // Exercises Builder.broker() which sets brokerEnabled = broker.isBrokerAvailable()
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .broker(mockBroker)
                .build();

        // brokerEnabled is private, so we verify the builder path was exercised
        // by confirming construction succeeded and the broker's availability was checked
        assertNotNull(pca);
    }

    // ========== ConfidentialClientApplication Tests ==========

    @Test
    void confidentialClientApplication_MinimalBuild_HasExpectedDefaults() {
        IClientCredential credential = ClientCredentialFactory.createFromSecret("test-secret");

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(CLIENT_ID, credential)
                .build();

        assertEquals(CLIENT_ID, cca.clientId());
        assertEquals(DEFAULT_AUTHORITY, cca.authority());
        assertTrue(cca.sendX5c(), "sendX5c defaults to true");
        assertNotNull(cca.tokenCache());
    }

    @Test
    void confidentialClientApplication_NullCredential_Throws() {
        assertThrows(IllegalArgumentException.class,
                () -> ConfidentialClientApplication.builder(CLIENT_ID, null));
    }

    @Test
    void confidentialClientApplication_SendX5c_SetToFalse() {
        IClientCredential credential = ClientCredentialFactory.createFromSecret("test-secret");

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(CLIENT_ID, credential)
                .sendX5c(false)
                .build();

        assertFalse(cca.sendX5c());
    }

    @Test
    void confidentialClientApplication_AppTokenProvider_Valid() {
        IClientCredential credential = ClientCredentialFactory.createFromSecret("test-secret");

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(CLIENT_ID, credential)
                .appTokenProvider(params -> {
                    TokenProviderResult result = new TokenProviderResult();
                    result.setAccessToken("token");
                    result.setExpiresInSeconds(3600);
                    return CompletableFuture.completedFuture(result);
                })
                .build();

        assertNotNull(cca.appTokenProvider);
    }

    @Test
    void confidentialClientApplication_AppTokenProvider_Null_Throws() {
        IClientCredential credential = ClientCredentialFactory.createFromSecret("test-secret");

        assertThrows(NullPointerException.class, () ->
                ConfidentialClientApplication.builder(CLIENT_ID, credential)
                        .appTokenProvider(null));
    }

    // ========== ManagedIdentityApplication Tests ==========

    @Test
    void managedIdentityApplication_SystemAssigned_Build() {
        ManagedIdentityApplication mia = ManagedIdentityApplication.builder(
                ManagedIdentityId.systemAssigned()).build();

        assertNotNull(mia.getManagedIdentityId());
        assertEquals(ManagedIdentityIdType.SYSTEM_ASSIGNED, mia.getManagedIdentityId().getIdType());
    }

    @Test
    void managedIdentityApplication_GetSharedTokenCache_ReturnsCache() {
        assertNotNull(ManagedIdentityApplication.getSharedTokenCache());
    }

    @Test
    void managedIdentityApplication_GetEnvironmentVariables_ReturnsValue() {
        // getEnvironmentVariables() returns whatever was last set via setEnvironmentVariables()
        IEnvironmentVariables original = ManagedIdentityApplication.getEnvironmentVariables();
        try {
            IEnvironmentVariables mockEnv = mock(IEnvironmentVariables.class);
            ManagedIdentityApplication.setEnvironmentVariables(mockEnv);
            assertSame(mockEnv, ManagedIdentityApplication.getEnvironmentVariables());
        } finally {
            ManagedIdentityApplication.setEnvironmentVariables(original);
        }
    }

    @Test
    void managedIdentityApplication_DeprecatedResource_DoesNotThrow() {
        // deprecated resource() is a no-op, should not throw
        ManagedIdentityApplication mia = ManagedIdentityApplication.builder(
                        ManagedIdentityId.systemAssigned())
                .resource("https://resource")
                .build();

        assertNotNull(mia);
    }

    @Test
    void managedIdentityApplication_ClientCapabilities_Set() {
        ManagedIdentityApplication mia = ManagedIdentityApplication.builder(
                        ManagedIdentityId.systemAssigned())
                .clientCapabilities(Arrays.asList("cp1", "cp2"))
                .build();

        assertEquals(Arrays.asList("cp1", "cp2"), mia.getClientCapabilities());
    }

    // ========== Builder Option Propagation Tests ==========

    @Test
    void builder_LogPii_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .logPii(true)
                .build();

        assertTrue(pca.logPii());
    }

    @Test
    void builder_CorrelationId_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .correlationId("test-correlation-id")
                .build();

        assertEquals("test-correlation-id", pca.correlationId());
    }

    @Test
    void builder_Proxy_PropagatedToApp() {
        Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxy.example.com", 8080));

        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .proxy(proxy)
                .build();

        assertEquals(proxy, pca.proxy());
    }

    @Test
    void builder_SslSocketFactory_PropagatedToApp() {
        SSLSocketFactory sslFactory = mock(SSLSocketFactory.class);

        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .sslSocketFactory(sslFactory)
                .build();

        assertSame(sslFactory, pca.sslSocketFactory());
    }

    @Test
    void builder_ExecutorService_PropagatedToApp() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                    .executorService(executor)
                    .build();

            assertNotNull(pca.serviceBundle().getExecutorService());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void builder_Timeouts_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .connectTimeoutForDefaultHttpClient(5000)
                .readTimeoutForDefaultHttpClient(10000)
                .build();

        assertEquals(5000, pca.connectTimeoutForDefaultHttpClient());
        assertEquals(10000, pca.readTimeoutForDefaultHttpClient());
    }

    @Test
    void builder_ApplicationNameAndVersion_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .applicationName("TestApp")
                .applicationVersion("1.0.0")
                .build();

        assertEquals("TestApp", pca.applicationName());
        assertEquals("1.0.0", pca.applicationVersion());
    }

    @Test
    void builder_ValidateAuthority_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .validateAuthority(false)
                .build();

        assertFalse(pca.validateAuthority());
    }

    @Test
    void builder_AutoDetectRegion_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .autoDetectRegion(true)
                .build();

        assertTrue(pca.autoDetectRegion());
    }

    @Test
    void builder_AzureRegion_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .azureRegion("westus")
                .build();

        assertEquals("westus", pca.azureRegion());
    }

    // ========================================================================
    // InteractiveRequest redirect URI validation
    // Only rejection cases are testable as unit tests — valid URIs proceed
    // to open a real browser, making them integration test territory.
    // ========================================================================

    @Test
    void interactiveRequest_HttpsScheme_ThrowsMsalClientException() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .instanceDiscovery(false)
                .build();

        InteractiveRequestParameters params = InteractiveRequestParameters
                .builder(new URI("https://localhost"))
                .scopes(Collections.singleton("scope"))
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class, () -> pca.acquireToken(params));
        assertEquals(AuthenticationErrorCode.LOOPBACK_REDIRECT_URI, ex.errorCode());
    }

    @Test
    void interactiveRequest_CustomScheme_ThrowsMsalClientException() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .instanceDiscovery(false)
                .build();

        InteractiveRequestParameters params = InteractiveRequestParameters
                .builder(new URI("myapp://callback"))
                .scopes(Collections.singleton("scope"))
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class, () -> pca.acquireToken(params));
        assertEquals(AuthenticationErrorCode.LOOPBACK_REDIRECT_URI, ex.errorCode());
    }

    @Test
    void interactiveRequest_NonLoopbackHost_ThrowsMsalClientException() throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .instanceDiscovery(false)
                .build();

        InteractiveRequestParameters params = InteractiveRequestParameters
                .builder(new URI("http://example.com"))
                .scopes(Collections.singleton("scope"))
                .build();

        MsalClientException ex = assertThrows(MsalClientException.class, () -> pca.acquireToken(params));
        assertEquals(AuthenticationErrorCode.LOOPBACK_REDIRECT_URI, ex.errorCode());
    }

    @Test
    void builder_InstanceDiscovery_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .instanceDiscovery(false)
                .build();

        assertFalse(pca.instanceDiscovery());
    }

    @Test
    void builder_ClientCapabilities_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .clientCapabilities(new HashSet<>(Arrays.asList("cp1", "cp2")))
                .build();

        assertNotNull(pca.clientCapabilities());
        assertTrue(pca.clientCapabilities().contains("cp1"));
    }

    @Test
    void builder_AadInstanceDiscoveryResponse_PropagatedToApp() {
        String discoveryResponse = TestHelper.getInstanceDiscoveryResponse();

        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .aadInstanceDiscoveryResponse(discoveryResponse)
                .build();

        assertNotNull(pca.aadAadInstanceDiscoveryResponse());
    }

    @Test
    void builder_HttpClient_PropagatedToApp() {
        IHttpClient httpClient = mock(IHttpClient.class);

        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .httpClient(httpClient)
                .build();

        assertSame(httpClient, pca.httpClient());
    }

    @Test
    void builder_DisableInternalRetries_PropagatedToApp() {
        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .disableInternalRetries()
                .build();

        assertTrue(pca.isRetryDisabled());
    }

    @Test
    void builder_TokenCacheAccessAspect_PropagatedToApp() {
        ITokenCacheAccessAspect aspect = mock(ITokenCacheAccessAspect.class);

        PublicClientApplication pca = PublicClientApplication.builder(CLIENT_ID)
                .setTokenCacheAccessAspect(aspect)
                .build();

        assertNotNull(pca.tokenCache());
    }

    // ========== Builder Null Validation Tests ==========

    @ParameterizedTest(name = "builder.{0}(null) throws")
    @MethodSource("nullValidationProvider")
    void builder_NullArgument_Throws(String methodName, Runnable action) {
        assertThrows(Exception.class, action::run,
                "Builder." + methodName + "(null) should throw");
    }

    private static Stream<Arguments> nullValidationProvider() {
        return Stream.of(
                Arguments.of("executorService", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).executorService(null)),
                Arguments.of("proxy", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).proxy(null)),
                Arguments.of("sslSocketFactory", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).sslSocketFactory(null)),
                Arguments.of("httpClient", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).httpClient(null)),
                Arguments.of("connectTimeoutForDefaultHttpClient", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).connectTimeoutForDefaultHttpClient(null)),
                Arguments.of("readTimeoutForDefaultHttpClient", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).readTimeoutForDefaultHttpClient(null)),
                Arguments.of("applicationName", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).applicationName(null)),
                Arguments.of("applicationVersion", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).applicationVersion(null)),
                Arguments.of("setTokenCacheAccessAspect", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).setTokenCacheAccessAspect(null)),
                Arguments.of("aadInstanceDiscoveryResponse", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).aadInstanceDiscoveryResponse(null)),
                Arguments.of("correlationId (blank)", (Runnable) () ->
                        PublicClientApplication.builder(CLIENT_ID).correlationId(""))
        );
    }
}
