// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClientSignedAssertionMtlsTest {
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant/";
    private static final String SCOPE = "https://graph.microsoft.com/.default";

    @Test
    void noCertificateWithoutPopUsesOrdinaryAssertionBehaviorAndCallbackTiming()
            throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("bearer", "Bearer"));
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options ->
                signedAssertion("assertion-" + calls.incrementAndGet(), null));

        IAuthenticationResult first = app.acquireToken(parameters(false, false)).get();
        IAuthenticationResult cached = app.acquireToken(parameters(false, false)).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(1)).send(request.capture());
        assertEquals(3, calls.get());
        assertEquals("login.microsoftonline.com", request.getValue().url().getHost());
        assertTrue(request.getValue().followRedirects());
        assertNull(request.getValue().sslContext());
        assertEquals("assertion-2", parameter(request.getValue().body(), "client_assertion"));
        assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_BEARER,
                parameter(request.getValue().body(), "client_assertion_type"));
        assertNull(parameter(request.getValue().body(), "token_type"));
        assertEquals("Bearer", first.tokenType());
        assertNull(first.mtlsBindingContext());
        assertEquals(TokenSource.CACHE, cached.metadata().tokenSource());
    }

    @Test
    void noCertificateWithPopFailsLocallyWithDotNetErrorContract() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options -> {
            calls.incrementAndGet();
            return signedAssertion("assertion", null);
        });

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(parameters(true, true)).get());

        assertEquals(1, calls.get());
        assertTrue(failure.getCause() instanceof MsalClientException);
        assertEquals(AuthenticationErrorCode.MTLS_CERTIFICATE_NOT_PROVIDED,
                ((MsalClientException) failure.getCause()).errorCode());
        assertTrue(failure.getCause().getMessage().contains(
                "mTLS Proof-of-Possession requires a certificate for this request."));
        verify(client, times(0)).send(any(HttpRequest.class));
    }

    @Test
    void certificateWithoutPopUsesMtlsAndJwtPopButReturnsUnboundBearer()
            throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("bearer", "Bearer"));
        AtomicInteger calls = new AtomicInteger();
        IClientCertificate certificate = providerBackedCertificate();
        ConfidentialClientApplication app = application(client, options ->
                signedAssertion("assertion-" + calls.incrementAndGet(), certificate));

        IAuthenticationResult first = app.acquireToken(parameters(false, false)).get();
        IAuthenticationResult cached = app.acquireToken(parameters(false, false)).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(1)).send(request.capture());
        assertEquals(3, calls.get());
        assertEquals("mtlsauth.microsoft.com", request.getValue().url().getHost());
        assertFalse(request.getValue().followRedirects());
        assertNotNull(request.getValue().sslContext());
        assertEquals("assertion-2", parameter(request.getValue().body(), "client_assertion"));
        assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_POP,
                parameter(request.getValue().body(), "client_assertion_type"));
        assertNull(parameter(request.getValue().body(), "token_type"));
        assertEquals("Bearer", first.tokenType());
        assertNull(first.bindingCertificate());
        assertNull(first.mtlsBindingContext());
        assertEquals(TokenSource.CACHE, cached.metadata().tokenSource());
        assertNull(cached.mtlsBindingContext());
    }

    @Test
    void certificateWithPopUsesJwtPopPartitionsCacheAndRestoresBinding()
            throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("token-a", "mtls_pop"))
                .thenReturn(response("token-b", "mtls_pop"));
        IClientCertificate certificateA = providerBackedCertificate();
        IClientCertificate certificateB = renewedCertificateWithSamePrivateKey();
        AtomicReference<IClientCertificate> currentCertificate =
                new AtomicReference<>(certificateA);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options ->
                signedAssertion("assertion-" + calls.incrementAndGet(),
                        currentCertificate.get()));

        IAuthenticationResult a1 = app.acquireToken(parameters(true, false)).get();
        currentCertificate.set(certificateB);
        IAuthenticationResult b = app.acquireToken(parameters(true, false)).get();
        currentCertificate.set(certificateA);
        IAuthenticationResult a2 = app.acquireToken(parameters(true, false)).get();

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(2)).send(requests.capture());
        assertEquals(5, calls.get());
        for (HttpRequest request : requests.getAllValues()) {
            assertEquals("mtlsauth.microsoft.com", request.url().getHost());
            assertFalse(request.followRedirects());
            assertNotNull(request.sslContext());
            assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_POP,
                    parameter(request.body(), "client_assertion_type"));
            assertEquals("mtls_pop", parameter(request.body(), "token_type"));
            assertNull(parameter(request.body(), "req_cnf"));
        }
        assertEquals("assertion-2",
                parameter(requests.getAllValues().get(0).body(), "client_assertion"));
        assertEquals("assertion-4",
                parameter(requests.getAllValues().get(1).body(), "client_assertion"));
        assertEquals("token-a", a1.accessToken());
        assertEquals("token-b", b.accessToken());
        assertEquals("token-a", a2.accessToken());
        assertEquals(TokenSource.CACHE, a2.metadata().tokenSource());
        assertNotNull(a1.mtlsBindingContext());
        assertNotNull(b.mtlsBindingContext());
        assertNotNull(a2.mtlsBindingContext());
        assertEquals(a1.mtlsBindingContext().keyId(), a2.mtlsBindingContext().keyId());
        assertFalse(a1.mtlsBindingContext().keyId().equals(b.mtlsBindingContext().keyId()));
    }

    @Test
    void runtimeAssertionIsNotRewrittenAndRuntimeCertificateControlsAssertionType()
            throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("bearer", "Bearer"));
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options -> {
            int call = calls.incrementAndGet();
            return signedAssertion(
                    call == 1 ? "preflight" : "runtime.assertion.signature",
                    call == 1 ? providerBackedCertificate() : null);
        });

        app.acquireToken(parameters(false, true)).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture());
        assertEquals(2, calls.get());
        assertEquals("mtlsauth.microsoft.com", request.getValue().url().getHost());
        assertEquals("runtime.assertion.signature",
                parameter(request.getValue().body(), "client_assertion"));
        assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_BEARER,
                parameter(request.getValue().body(), "client_assertion_type"));
    }

    @Test
    void nullOrEmptySignedAssertionUsesDotNetErrorCode() throws Exception {
        for (ClientSignedAssertion invalid : new ClientSignedAssertion[]{
                null, signedAssertion(null, null), signedAssertion("", null)}) {
            DefaultHttpClient client = mock(DefaultHttpClient.class);
            ConfidentialClientApplication app = application(client, options -> invalid);

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> app.acquireToken(parameters(false, true)).get());

            assertEquals(AuthenticationErrorCode.INVALID_CLIENT_ASSERTION,
                    ((MsalClientException) failure.getCause()).errorCode());
            verify(client, times(0)).send(any(HttpRequest.class));
        }
    }

    private static ConfidentialClientApplication application(
            IHttpClient client, IClientSignedAssertionProvider provider) throws Exception {
        return ConfidentialClientApplication
                .builder("client",
                        ClientCredentialFactory.createFromSignedAssertionCallback(provider))
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(client)
                .build();
    }

    private static ClientCredentialParameters parameters(boolean pop, boolean skipCache) {
        ClientCredentialParameters.ClientCredentialParametersBuilder builder =
                ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                        .skipCache(skipCache);
        return pop ? builder.mtlsProofOfPossession().build() : builder.build();
    }

    private static ClientSignedAssertion signedAssertion(
            String assertion, IClientCertificate certificate) {
        ClientSignedAssertion result = new ClientSignedAssertion();
        result.setAssertion(assertion);
        result.setTokenBindingCertificate(certificate);
        return result;
    }

    private static IClientCertificate providerBackedCertificate() {
        assertNull(TestHelper.getPrivateKey().getEncoded());
        return ClientCredentialFactory.createFromCertificate(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());
    }

    private static IClientCertificate renewedCertificateWithSamePrivateKey() {
        return new IClientCertificate() {
            @Override
            public PrivateKey privateKey() {
                return TestHelper.getPrivateKey();
            }

            @Override
            public String publicCertificateHash()
                    throws CertificateEncodingException, NoSuchAlgorithmException {
                return null;
            }

            @Override
            public List<String> getEncodedPublicKeyCertificateChain()
                    throws CertificateEncodingException {
                byte[] renewedDer = TestHelper.getX509Cert().getEncoded().clone();
                renewedDer[renewedDer.length - 1] ^= 1;
                return Collections.singletonList(
                        Base64.getEncoder().encodeToString(renewedDer));
            }
        };
    }

    private static String parameter(String body, String name) throws Exception {
        for (String pair : body.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts[0].equals(name)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8.name());
            }
        }
        return null;
    }

    private static HttpResponse response(String accessToken, String tokenType) {
        HttpResponse response = new HttpResponse();
        response.statusCode(HttpStatus.HTTP_OK);
        response.body("{\"access_token\":\"" + accessToken
                + "\",\"expires_in\":3600,\"token_type\":\"" + tokenType + "\"}");
        response.addHeaders(Collections.singletonMap(
                "Content-Type", Collections.singletonList("application/json")));
        return response;
    }
}
