// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.InputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the app-level {@code sendCertificateOverMtls} flag (Bearer-over-mTLS).
 *
 * <p>Bearer-over-mTLS presents the SN/I certificate as the client TLS certificate on the handshake to
 * the token endpoint and routes to the mTLS endpoint, but the identity provider returns a PLAIN
 * {@code Bearer} access token that is NOT bound to the certificate. This is distinct from mTLS
 * Proof-of-Possession (which binds the token, {@code token_type=mtls_pop}, and fences the cache by
 * certificate thumbprint) and from the ordinary certificate flow (which signs a {@code private_key_jwt}
 * to the regular {@code login.*} endpoint and never presents the certificate on the TLS handshake).
 *
 * <p>Mirrors MSAL.NET's {@code CertificateOptions.SendCertificateOverMtls} coverage, adapted to msal4j
 * idiom and the {@link MtlsProofOfPossessionTest} test style.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BearerOverMtlsTest {

    private static final String PKCS12_RESOURCE = "/mtls_test_cert.p12";
    private static final String PKCS12_PASSWORD = "password";
    private static final String AUTHORITY = "https://login.microsoftonline.com/contoso.onmicrosoft.com/";
    private static final String SCOPE = "https://graph.microsoft.com/.default";
    private static final String GLOBAL_MTLS_HOST = "mtlsauth.microsoft.com";

    private IClientCertificate certificate;

    // Runs token acquisition on the calling thread so Mockito's thread-local mockConstruction intercepts
    // the mTLS DefaultHttpClient (which msal4j otherwise builds on a ForkJoinPool worker thread).
    private static final ExecutorService SAME_THREAD_EXECUTOR = new SameThreadExecutorService();

    private static final class SameThreadExecutorService extends AbstractExecutorService {
        @Override
        public void execute(Runnable command) {
            command.run();
        }

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return Collections.emptyList();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }
    }

    @BeforeAll
    void setUp() throws Exception {
        try (InputStream pkcs12 = getClass().getResourceAsStream(PKCS12_RESOURCE)) {
            assertNotNull(pkcs12, "Test PKCS12 resource " + PKCS12_RESOURCE + " should be present");
            certificate = ClientCredentialFactory.createFromCertificate(pkcs12, PKCS12_PASSWORD);
        }
    }

    private ConfidentialClientApplication.Builder baseCertAppBuilder() throws Exception {
        return ConfidentialClientApplication.builder("clientId", certificate)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .executorService(SAME_THREAD_EXECUTOR)
                .httpClient(mock(IHttpClient.class));
    }

    private static HttpResponse successResponse(String accessToken, String tokenType) {
        HashMap<String, String> values = new HashMap<>();
        values.put("access_token", accessToken);
        values.put("token_type", tokenType);
        return TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(values));
    }

    private static Map<String, String> parseFormBody(String body) {
        Map<String, String> params = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return params;
        }
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) {
                params.put(urlDecode(pair), "");
            } else {
                params.put(urlDecode(pair.substring(0, idx)), urlDecode(pair.substring(idx + 1)));
            }
        }
        return params;
    }

    private static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    // Decodes the JWT header of a client_assertion and asserts the x5c (certificate chain) claim is present.
    private static boolean assertionHeaderHasX5c(String clientAssertion) {
        assertNotNull(clientAssertion, "client_assertion must be present");
        String[] segments = clientAssertion.split("\\.");
        assertTrue(segments.length >= 2, "client_assertion should be a JWT");
        String headerJson = new String(Base64.getUrlDecoder().decode(segments[0]), StandardCharsets.UTF_8);
        return headerJson.contains("\"x5c\"");
    }

    // ---------------------------------------------------------------------------------------------
    // A. Config / builder
    // ---------------------------------------------------------------------------------------------

    @Test
    void sendCertificateOverMtls_defaultsFalse() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder().build();

        assertFalse(app.sendCertificateOverMtls(),
                "sendCertificateOverMtls must default to false (zero behavior change when unset)");
    }

    @Test
    void sendCertificateOverMtls_storedAndReturnedByGetter() throws Exception {
        ConfidentialClientApplication enabled = baseCertAppBuilder().sendCertificateOverMtls(true).build();
        assertTrue(enabled.sendCertificateOverMtls(), "flag set to true should be reported by the getter");

        ConfidentialClientApplication disabled = baseCertAppBuilder().sendCertificateOverMtls(false).build();
        assertFalse(disabled.sendCertificateOverMtls(), "flag set to false should be reported by the getter");
    }

    @Test
    void sendCertificateOverMtls_nonCertificateCredential_failsFastAtBuild() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                ConfidentialClientApplication.builder("clientId", new ClientSecret("secret"))
                        .authority(AUTHORITY)
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .sendCertificateOverMtls(true)
                        .build());

        assertEquals(AuthenticationErrorCode.CERTIFICATE_REQUIRED_FOR_MTLS, ex.errorCode());
        assertTrue(ex.getMessage().toLowerCase().contains("sendcertificateovermtls"),
                "error message should name the sendCertificateOverMtls flag");
    }

    // ---------------------------------------------------------------------------------------------
    // B. Client-credentials behavior
    // ---------------------------------------------------------------------------------------------

    @Test
    void clientCredentials_bearerOverMtls_targetsMtlsEndpoint_forcesX5cAssertion_returnsBearer() throws Exception {
        // sendX5c(false) proves the x5c is FORCED on for Bearer-over-mTLS regardless of the app setting.
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .sendX5c(false)
                .build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            result = app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                    .skipCache(true)
                    .build()).get();

            assertEquals(1, mocked.constructed().size(), "Exactly one mTLS DefaultHttpClient should be built");
            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        // Endpoint: cert presented on the TLS handshake, host rewritten to the global mTLS host.
        assertEquals(GLOBAL_MTLS_HOST, captured.url().getHost());
        assertTrue(captured.url().getPath().contains("/contoso.onmicrosoft.com/oauth2/v2.0/token"));

        Map<String, String> body = parseFormBody(captured.body());
        // A plain-Bearer request over mTLS: client_assertion (jwt-bearer) with x5c FORCED on, but NO PoP markers.
        assertEquals("client_credentials", body.get("grant_type"));
        assertNotNull(body.get("client_assertion"), "Bearer-over-mTLS still sends a client_assertion");
        assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_BEARER, body.get("client_assertion_type"));
        assertTrue(assertionHeaderHasX5c(body.get("client_assertion")),
                "Bearer-over-mTLS must FORCE the x5c chain on the assertion even when sendX5c(false)");
        assertFalse(body.containsKey("token_type"), "Bearer-over-mTLS must not request token_type=mtls_pop");
        assertFalse(body.containsKey("req_cnf"), "Bearer-over-mTLS must not send req_cnf");

        // Result: a plain Bearer token, not certificate-bound.
        assertEquals(TokenType.BEARER, result.metadata().tokenType());
    }

    @Test
    void clientCredentials_bearerOverMtls_regionConfigured_targetsRegionalMtlsEndpoint() throws Exception {
        // Pre-seed the regional instance metadata so no live IMDS/instance-discovery call is needed; region
        // rewriting only runs with instanceDiscovery(true), and is client-credentials only.
        String regionalLoginHost = "westus3.login.microsoft.com";
        AadInstanceDiscoveryProvider.cache.put(regionalLoginHost,
                new InstanceDiscoveryMetadataEntry(regionalLoginHost, "login.microsoftonline.com",
                        new HashSet<>(Arrays.asList(regionalLoginHost, "login.microsoftonline.com"))));
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId", certificate)
                    .authority(AUTHORITY)
                    .instanceDiscovery(true)
                    .validateAuthority(false)
                    .azureRegion("westus3")
                    .executorService(SAME_THREAD_EXECUTOR)
                    .httpClient(mock(IHttpClient.class))
                    .sendCertificateOverMtls(true)
                    .build();

            HttpRequest captured;
            try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                    (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

                app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                        .skipCache(true)
                        .build()).get();

                ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
                verify(mocked.constructed().get(0)).send(requestCaptor.capture());
                captured = requestCaptor.getValue();
            }

            assertEquals("westus3.mtlsauth.microsoft.com", captured.url().getHost(),
                    "region-configured Bearer-over-mTLS must target the regional mTLS host");
        } finally {
            AadInstanceDiscoveryProvider.cache.remove(regionalLoginHost);
        }
    }

    @Test
    void perRequestMtlsPop_overridesBearerOverMtlsFlag() throws Exception {
        // Per-request mtls_pop opt-in ALWAYS wins over the app-level Bearer-over-mTLS flag.
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("mtls-pop-token", "mtls_pop")))) {

            result = app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                    .mtlsProofOfPossession()
                    .skipCache(true)
                    .build()).get();

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals(GLOBAL_MTLS_HOST, captured.url().getHost());
        Map<String, String> body = parseFormBody(captured.body());
        // mtls_pop wins: PoP markers present, and NO client_assertion (cert resolved from the TLS handshake).
        assertEquals("mtls_pop", body.get("token_type"));
        assertFalse(body.containsKey("client_assertion"),
                "per-request mtls_pop must win: no client_assertion is sent");
        assertEquals(TokenType.MTLS_POP, result.metadata().tokenType());
        assertNotNull(result.metadata().bindingCertificate());
    }

    @Test
    void clientCredentials_bearerOverMtls_secondCall_servedFromCacheAsPlainBearer() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .build();

        IAuthenticationResult networkResult;
        IAuthenticationResult cachedResult;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            // First call: cache empty, goes to the (mocked) mTLS network and caches a PLAIN Bearer token.
            networkResult = app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                    .build()).get();
            // Second call: a normal Bearer cache lookup returns it (the entry is not thumbprint-fenced).
            cachedResult = app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                    .build()).get();

            assertEquals(1, mocked.constructed().size(),
                    "Second acquireToken must be served from the plain Bearer cache, not the network");
        }

        assertEquals(TokenType.BEARER, networkResult.metadata().tokenType());
        assertEquals(TokenType.BEARER, cachedResult.metadata().tokenType());
    }

    // ---------------------------------------------------------------------------------------------
    // C. User flows (OBO / refresh-token / authorization-code)
    // ---------------------------------------------------------------------------------------------

    @Test
    void onBehalfOf_bearerOverMtls_targetsMtlsEndpoint_forcesX5cAssertion_onBehalfOfGrant() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .sendX5c(false)
                .build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            result = app.acquireToken(OnBehalfOfParameters
                    .builder(Collections.singleton(SCOPE), new UserAssertion(TestHelper.signedAssertion))
                    .build()).get();

            assertEquals(1, mocked.constructed().size(), "Exactly one mTLS DefaultHttpClient should be built");
            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals(GLOBAL_MTLS_HOST, captured.url().getHost());
        Map<String, String> body = parseFormBody(captured.body());
        assertEquals("on_behalf_of", body.get("requested_token_use"), "OBO grant should be preserved");
        assertNotNull(body.get("client_assertion"), "Bearer-over-mTLS still sends a client_assertion for OBO");
        assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_BEARER, body.get("client_assertion_type"));
        assertTrue(assertionHeaderHasX5c(body.get("client_assertion")),
                "Bearer-over-mTLS must FORCE the x5c chain on the OBO client_assertion");
        assertFalse(body.containsKey("token_type"), "OBO Bearer-over-mTLS must not request token_type=mtls_pop");
        assertEquals(TokenType.BEARER, result.metadata().tokenType());
    }

    @Test
    void onBehalfOf_bearerOverMtls_regionConfigured_stillTargetsGlobalMtlsEndpoint() throws Exception {
        // Region is client-credentials-only in msal4j; user flows silently fall back to the global endpoint.
        // Bearer-over-mTLS therefore routes OBO to the GLOBAL mTLS host even when a region is configured.
        ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId", certificate)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .azureRegion("westus3")
                .executorService(SAME_THREAD_EXECUTOR)
                .httpClient(mock(IHttpClient.class))
                .sendCertificateOverMtls(true)
                .build();

        HttpRequest captured;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            app.acquireToken(OnBehalfOfParameters
                    .builder(Collections.singleton(SCOPE), new UserAssertion(TestHelper.signedAssertion))
                    .build()).get();

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals(GLOBAL_MTLS_HOST, captured.url().getHost(),
                "OBO ignores azureRegion (client-credentials only) and uses the global mTLS host");
    }

    @Test
    void refreshToken_bearerOverMtls_targetsMtlsEndpoint_forcesX5cAssertion_refreshTokenGrant() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .sendX5c(false)
                .build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            result = app.acquireToken(RefreshTokenParameters
                    .builder(Collections.singleton(SCOPE), "a-refresh-token")
                    .build()).get();

            assertEquals(1, mocked.constructed().size(), "Exactly one mTLS DefaultHttpClient should be built");
            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals(GLOBAL_MTLS_HOST, captured.url().getHost());
        Map<String, String> body = parseFormBody(captured.body());
        assertEquals("refresh_token", body.get("grant_type"));
        assertNotNull(body.get("client_assertion"), "Bearer-over-mTLS still sends a client_assertion for refresh");
        assertTrue(assertionHeaderHasX5c(body.get("client_assertion")),
                "Bearer-over-mTLS must FORCE the x5c chain on the refresh client_assertion");
        assertFalse(body.containsKey("token_type"), "refresh Bearer-over-mTLS must not request token_type=mtls_pop");
        assertEquals(TokenType.BEARER, result.metadata().tokenType());
    }

    @Test
    void authorizationCode_bearerOverMtls_targetsMtlsEndpoint_forcesX5cAssertion_authorizationCodeGrant() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .sendX5c(false)
                .build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            result = app.acquireToken(AuthorizationCodeParameters
                    .builder("an-auth-code", new URI("http://localhost:8080"))
                    .scopes(Collections.singleton(SCOPE))
                    .build()).get();

            assertEquals(1, mocked.constructed().size(), "Exactly one mTLS DefaultHttpClient should be built");
            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals(GLOBAL_MTLS_HOST, captured.url().getHost());
        Map<String, String> body = parseFormBody(captured.body());
        assertEquals("authorization_code", body.get("grant_type"));
        assertNotNull(body.get("client_assertion"), "Bearer-over-mTLS still sends a client_assertion for auth-code");
        assertTrue(assertionHeaderHasX5c(body.get("client_assertion")),
                "Bearer-over-mTLS must FORCE the x5c chain on the auth-code client_assertion");
        assertFalse(body.containsKey("token_type"), "auth-code Bearer-over-mTLS must not request token_type=mtls_pop");
        assertEquals(TokenType.BEARER, result.metadata().tokenType());
    }

    @Test
    void onBehalfOf_bearerOverMtls_secondCall_servedFromCache_noCrash() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder()
                .sendCertificateOverMtls(true)
                .build();

        OnBehalfOfParameters params = OnBehalfOfParameters
                .builder(Collections.singleton(SCOPE), new UserAssertion(TestHelper.signedAssertion))
                .build();

        IAuthenticationResult networkResult;
        IAuthenticationResult cachedResult;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

            // First call goes to the (mocked) mTLS network; second must be served from the cache without
            // crashing on region/instance-metadata resolution for the mTLS-derived environment.
            networkResult = app.acquireToken(params).get();
            cachedResult = app.acquireToken(params).get();

            assertEquals(1, mocked.constructed().size(),
                    "Second OBO acquireToken must be served from the cache, not the network");
        }

        assertEquals(TokenType.BEARER, networkResult.metadata().tokenType());
        assertEquals(TokenType.BEARER, cachedResult.metadata().tokenType());
    }

    @Test
    void onBehalfOf_bearerOverMtls_secondCall_withInstanceDiscovery_cachedUnderLoginHost_noCrash() throws Exception {
        // Regression guard for the Bearer-over-mTLS 2nd-call cache path with instance discovery ENABLED
        // (mirrors .NET OboFlow_WithSendCertificateOverMtls_SecondCallDoesNotCrashAsync). In .NET the AT was
        // cached under Environment = the rewritten mtlsauth host, so the 2nd call fed mtlsauth.* into
        // instance/region metadata discovery — which only accepts login.* hosts — and threw. In msal4j this
        // holds by construction: only the local token-endpoint URL is rewritten to mtlsauth, while the
        // request authority (and therefore the cached AT's environment) stays the login host. The
        // instanceDiscovery(false) sibling test skips discovery entirely, so this one enables it (seeding the
        // login-host metadata to avoid a live IMDS call) to actually exercise the resolution path.
        String loginHost = "login.microsoftonline.com";
        AadInstanceDiscoveryProvider.cache.put(loginHost,
                new InstanceDiscoveryMetadataEntry(loginHost, loginHost,
                        new HashSet<>(Arrays.asList(loginHost))));
        try {
            ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId", certificate)
                    .authority(AUTHORITY)
                    .instanceDiscovery(true)
                    .validateAuthority(false)
                    .executorService(SAME_THREAD_EXECUTOR)
                    .httpClient(mock(IHttpClient.class))
                    .sendCertificateOverMtls(true)
                    .build();

            OnBehalfOfParameters params = OnBehalfOfParameters
                    .builder(Collections.singleton(SCOPE), new UserAssertion(TestHelper.signedAssertion))
                    .build();

            IAuthenticationResult cachedResult;
            try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                    (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer")))) {

                app.acquireToken(params).get();                 // 1st: mocked mTLS network, caches plain Bearer
                cachedResult = app.acquireToken(params).get();  // 2nd: must serve from cache, no metadata crash

                assertEquals(1, mocked.constructed().size(),
                        "Second OBO call must be served from the cache even with instance discovery enabled");
            }

            assertEquals(TokenType.BEARER, cachedResult.metadata().tokenType());

            // The AT is cached under the LOGIN host, never the rewritten mtlsauth host — the property that
            // keeps the 2nd-call metadata resolution valid (the cache key embeds the environment).
            assertEquals(1, app.tokenCache.accessTokens.size());
            String cacheKey = app.tokenCache.accessTokens.keySet().iterator().next();
            assertTrue(cacheKey.contains(loginHost),
                    "Bearer-over-mTLS AT must be cached under the login host, got key: " + cacheKey);
            assertFalse(cacheKey.contains("mtlsauth"),
                    "Bearer-over-mTLS AT must NOT be cached under the rewritten mtlsauth host, got key: " + cacheKey);
        } finally {
            AadInstanceDiscoveryProvider.cache.remove(loginHost);
        }
    }

    @Test
    void onBehalfOf_withoutFlag_usesLoginEndpoint() throws Exception {
        // Negative control: with the flag OFF, OBO uses the standard login endpoint and never presents the
        // certificate on an mTLS handshake (no mTLS DefaultHttpClient is constructed).
        IHttpClient appHttpClient = mock(IHttpClient.class);
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(appHttpClient.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token", "Bearer"));

        ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId", certificate)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .executorService(SAME_THREAD_EXECUTOR)
                .httpClient(appHttpClient)
                .build();

        app.acquireToken(OnBehalfOfParameters
                .builder(Collections.singleton(SCOPE), new UserAssertion(TestHelper.signedAssertion))
                .build()).get();

        verify(appHttpClient).send(requestCaptor.capture());
        HttpRequest captured = requestCaptor.getValue();

        assertEquals("login.microsoftonline.com", captured.url().getHost(),
                "without the flag, OBO must use the standard login endpoint");
        Map<String, String> body = parseFormBody(captured.body());
        assertEquals("on_behalf_of", body.get("requested_token_use"));
        assertNotNull(body.get("client_assertion"), "the standard OBO path still sends a client_assertion");
    }
}
