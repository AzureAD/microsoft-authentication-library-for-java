// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MtlsProofOfPossessionTest {

    private static final String PKCS12_RESOURCE = "/mtls_test_cert.p12";
    private static final String PKCS12_PASSWORD = "password";
    private static final String AUTHORITY = "https://login.microsoftonline.com/contoso.onmicrosoft.com/";
    private static final String JWT_POP_ASSERTION_TYPE =
            "urn:ietf:params:oauth:client-assertion-type:jwt-pop";

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

    private static HttpResponse successResponse(String accessToken) {
        HashMap<String, String> values = new HashMap<>();
        values.put("access_token", accessToken);
        return TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(values));
    }

    @Test
    void directSniCert_mtlsPop_targetsMtlsEndpoint_omitsClientAssertion_returnsMtlsPopToken() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder().build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("mtls-pop-token")))) {

            result = app.acquireToken(ClientCredentialParameters.builder(Collections.singleton("https://graph.microsoft.com/.default"))
                    .mtlsProofOfPossession()
                    .skipCache(true)
                    .build()).get();

            assertEquals(1, mocked.constructed().size(), "Exactly one mTLS DefaultHttpClient should be built");
            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        // Endpoint: host rewritten to the global mTLS host, tenanted token path preserved.
        assertEquals("mtlsauth.microsoft.com", captured.url().getHost());
        assertTrue(captured.url().getPath().contains("/contoso.onmicrosoft.com/oauth2/v2.0/token"));

        // Body: token_type=mtls_pop, and NO client_assertion (ESTS resolves SN/I trust from the TLS cert).
        String body = captured.body();
        assertTrue(body.contains("token_type=mtls_pop"), "body should request token_type=mtls_pop");
        assertFalse(body.contains("client_assertion"), "vanilla SN/I mTLS PoP must not send a client_assertion");
        assertFalse(body.contains("req_cnf"), "mTLS PoP must not send req_cnf");

        // Result: cert-bound PoP token, binding cert surfaced as public material only.
        assertEquals(TokenType.MTLS_POP, result.metadata().tokenType());
        assertNotNull(result.metadata().bindingCertificate());
        assertEquals(MtlsClientCertificateHelper.computeCertificateKeyId(certificate),
                result.metadata().bindingCertificate().thumbprintSha256());
    }

    @Test
    void ficLeg2_assertionWithBindingCert_usesJwtPopAssertionType() throws Exception {
        String leg1Token = TestHelper.signedAssertion;

        ConfidentialClientApplication app = ConfidentialClientApplication.builder("agentClientId",
                        ClientCredentialFactory.createFromClientAssertion(leg1Token))
                .authority(AUTHORITY)
                .mtlsBindingCertificate(certificate)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .executorService(SAME_THREAD_EXECUTOR)
                .httpClient(mock(IHttpClient.class))
                .build();

        HttpRequest captured;
        IAuthenticationResult result;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("leg2-token")))) {

            result = app.acquireToken(ClientCredentialParameters.builder(Collections.singleton("https://graph.microsoft.com/.default"))
                    .mtlsProofOfPossession()
                    .skipCache(true)
                    .build()).get();

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals("mtlsauth.microsoft.com", captured.url().getHost());

        String body = captured.body();
        assertTrue(body.contains("client_assertion=" + leg1Token), "Leg 2 must authenticate with the Leg-1 token");
        assertTrue(body.contains("jwt-pop"), "Leg 2 must use the jwt-pop client_assertion_type");
        assertTrue(body.contains("token_type=mtls_pop"));

        assertEquals(TokenType.MTLS_POP, result.metadata().tokenType());
        assertEquals(MtlsClientCertificateHelper.computeCertificateKeyId(certificate),
                result.metadata().bindingCertificate().thumbprintSha256());
    }

    @Test
    void bearerCert_backwardCompatible_usesLoginEndpointAndJwtBearerAssertion() throws Exception {
        IHttpClient appHttpClient = mock(IHttpClient.class);
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        when(appHttpClient.send(any(HttpRequest.class))).thenReturn(successResponse("bearer-token"));

        ConfidentialClientApplication app = ConfidentialClientApplication.builder("clientId", certificate)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .executorService(SAME_THREAD_EXECUTOR)
                .httpClient(appHttpClient)
                .build();

        IAuthenticationResult result = app.acquireToken(
                ClientCredentialParameters.builder(Collections.singleton("https://graph.microsoft.com/.default"))
                        .skipCache(true)
                        .build()).get();

        verify(appHttpClient).send(requestCaptor.capture());
        HttpRequest captured = requestCaptor.getValue();

        // Unchanged Bearer path: standard login endpoint, x5c client assertion (jwt-bearer), no mTLS markers.
        assertEquals("login.microsoftonline.com", captured.url().getHost());
        String body = captured.body();
        assertTrue(body.contains("client_assertion"), "Bearer SN/I path still sends a client_assertion");
        assertTrue(body.contains("jwt-bearer"), "Bearer path uses the jwt-bearer client_assertion_type");
        assertFalse(body.contains("token_type=mtls_pop"), "Bearer path must not request mTLS PoP");

        assertEquals(TokenType.BEARER, result.metadata().tokenType());
    }

    @Test
    void cacheKey_isolatesBearerFromMtlsPopAndByCertificate() {
        ClientCredentialParameters bearer = ClientCredentialParameters
                .builder(Collections.singleton("scope")).build();

        ClientCredentialParameters mtls = ClientCredentialParameters
                .builder(Collections.singleton("scope")).mtlsProofOfPossession().build();
        mtls.bindingCertificateKeyId(MtlsClientCertificateHelper.computeCertificateKeyId(certificate));

        ClientCredentialParameters mtlsOtherCert = ClientCredentialParameters
                .builder(Collections.singleton("scope")).mtlsProofOfPossession().build();
        mtlsOtherCert.bindingCertificateKeyId("a-different-cert-key-id");

        String bearerHash = bearer.computeExtCacheKeyHash();
        String mtlsHash = mtls.computeExtCacheKeyHash();
        String mtlsOtherHash = mtlsOtherCert.computeExtCacheKeyHash();

        assertNotEquals(bearerHash, mtlsHash, "Bearer and mTLS PoP tokens must not alias in the cache");
        assertNotEquals(mtlsHash, mtlsOtherHash, "mTLS PoP tokens bound to different certs must not alias");
    }

    @Test
    void mtlsPop_composesWithFmiPath_forFicLeg1() throws Exception {
        ConfidentialClientApplication app = baseCertAppBuilder().build();

        HttpRequest captured;
        try (MockedConstruction<DefaultHttpClient> mocked = mockConstruction(DefaultHttpClient.class,
                (m, ctx) -> when(m.send(any(HttpRequest.class))).thenReturn(successResponse("fic-leg1-token")))) {

            app.acquireToken(ClientCredentialParameters.builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                    .fmiPath("SomeFmiPath/CredentialPath")
                    .mtlsProofOfPossession()
                    .skipCache(true)
                    .build()).get();

            ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
            verify(mocked.constructed().get(0)).send(requestCaptor.capture());
            captured = requestCaptor.getValue();
        }

        assertEquals("mtlsauth.microsoft.com", captured.url().getHost());
        String body = captured.body();
        assertTrue(body.contains("token_type=mtls_pop"));
        assertTrue(body.contains("fmi_path"), "FIC Leg 1 should still send fmi_path alongside mTLS PoP");
        assertFalse(body.contains("client_assertion"), "FIC Leg 1 (cert) must not send a client_assertion");
    }
}
