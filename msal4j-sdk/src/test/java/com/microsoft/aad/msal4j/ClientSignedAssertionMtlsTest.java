// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @ParameterizedTest
    @ValueSource(strings = {"tenant", "common", "organizations", "consumers"})
    void noCertificateWithPopFailsBeforeMtlsAuthorityValidation(String tenant) throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options -> {
            calls.incrementAndGet();
            return signedAssertion("assertion", null);
        }, "https://login.microsoftonline.com/" + tenant + "/");

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

    @ParameterizedTest
    @ValueSource(strings = {"token_type", "TOKEN_TYPE", "req_cnf", "REQ_CNF"})
    void certificateWithoutPopRejectsProtocolOverrides(String name) throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        IClientCertificate certificate = providerBackedCertificate();
        ConfidentialClientApplication app = application(client,
                options -> signedAssertion("assertion", certificate));

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(ClientCredentialParameters.builder(
                                Collections.singleton(SCOPE))
                        .extraQueryParameters(Collections.singletonMap(name, "mtls_pop"))
                        .skipCache(true).build()).get());

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR,
                ((MsalClientException) failure.getCause()).errorCode());
        verify(client, times(0)).send(any(HttpRequest.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"mtls_pop", "PoP"})
    void certificateWithoutPopRejectsUnexpectedTokenTypesBeforeCaching(String tokenType)
            throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("unexpected", tokenType))
                .thenReturn(response("bearer", "Bearer"));
        IClientCertificate certificate = providerBackedCertificate();
        ConfidentialClientApplication app = application(client,
                options -> signedAssertion("assertion", certificate));

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(parameters(false, false)).get());
        assertEquals(AuthenticationErrorCode.TOKEN_TYPE_MISMATCH,
                ((MsalClientException) failure.getCause()).errorCode());

        assertEquals("bearer", app.acquireToken(parameters(false, false)).get().accessToken());
        IAuthenticationResult cached = app.acquireToken(parameters(false, false)).get();
        assertEquals("bearer", cached.accessToken());
        assertEquals(TokenSource.CACHE, cached.metadata().tokenSource());
        verify(client, times(2)).send(any(HttpRequest.class));
    }

    @Test
    void certificateCannotFirstAppearInNetworkCallback() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        IClientCertificate certificate = providerBackedCertificate();
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options ->
                signedAssertion("assertion", calls.incrementAndGet() == 1 ? null : certificate));

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(parameters(false, true)).get());

        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR,
                ((MsalClientException) failure.getCause()).errorCode());
        assertTrue(failure.getCause().getMessage().contains("after request preflight"));
        assertEquals(2, calls.get());
        verify(client, times(0)).send(any(HttpRequest.class));
    }

    @Test
    void popRequiresCertificateInNetworkCallbackDespitePreflightBinding() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        IClientCertificate certificate = providerBackedCertificate();
        AtomicInteger calls = new AtomicInteger();
        ConfidentialClientApplication app = application(client, options ->
                signedAssertion("assertion", calls.incrementAndGet() == 1 ? certificate : null));

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(parameters(true, true)).get());

        assertEquals(AuthenticationErrorCode.MTLS_CERTIFICATE_NOT_PROVIDED,
                ((MsalClientException) failure.getCause()).errorCode());
        assertEquals(2, calls.get());
        verify(client, times(0)).send(any(HttpRequest.class));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void networkCallbackReceivesSelectedTransportEndpoint(boolean pop) throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("token", pop ? "mtls_pop" : "Bearer"));
        IClientCertificate certificate = providerBackedCertificate();
        List<AssertionRequestOptions> contexts = new ArrayList<>();
        ConfidentialClientApplication app = application(client, options -> {
            contexts.add(options);
            return signedAssertion("unchanged.assertion.signature", certificate);
        });
        ClientCredentialParameters.ClientCredentialParametersBuilder builder =
                ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                        .tenant("override-tenant").skipCache(true);

        app.acquireToken(pop ? builder.mtlsProofOfPossession().build() : builder.build()).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture());
        assertEquals(2, contexts.size());
        assertEquals("https://login.microsoftonline.com/override-tenant/oauth2/v2.0/token",
                contexts.get(0).tokenEndpoint());
        assertEquals("https://mtlsauth.microsoft.com/override-tenant/oauth2/v2.0/token",
                contexts.get(1).tokenEndpoint());
        assertEquals(request.getValue().url().toString(), contexts.get(1).tokenEndpoint());
        assertEquals("client", contexts.get(1).clientId());
        assertEquals("unchanged.assertion.signature",
                parameter(request.getValue().body(), "client_assertion"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"obo", "authorization_code", "refresh_token"})
    void otherGrantsRejectBindingCertificatesButAllowOrdinaryAssertions(String grant)
            throws Exception {
        for (boolean certificateBound : new boolean[]{false, true}) {
            DefaultHttpClient client = mock(DefaultHttpClient.class);
            when(client.send(any(HttpRequest.class))).thenReturn(response("bearer", "Bearer"));
            IClientCertificate certificate = certificateBound ? providerBackedCertificate() : null;
            AtomicInteger calls = new AtomicInteger();
            ConfidentialClientApplication app = application(client, options -> {
                calls.incrementAndGet();
                return signedAssertion("assertion", certificate);
            });

            if (certificateBound) {
                ExecutionException failure = assertThrows(ExecutionException.class,
                        () -> acquireOtherGrant(app, grant).get());
                assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR,
                        ((MsalClientException) failure.getCause()).errorCode());
                assertTrue(failure.getCause().getMessage().contains(
                        "supported only for client credential requests"));
                verify(client, times(0)).send(any(HttpRequest.class));
            } else {
                assertEquals("bearer", acquireOtherGrant(app, grant).get().accessToken());
                ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
                verify(client).send(request.capture());
                assertEquals("login.microsoftonline.com", request.getValue().url().getHost());
                assertNull(request.getValue().sslContext());
                assertEquals(ClientAssertion.ASSERTION_TYPE_JWT_BEARER,
                        parameter(request.getValue().body(), "client_assertion_type"));
            }
            assertEquals(1, calls.get());
        }
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
        return application(client, provider, AUTHORITY);
    }

    private static ConfidentialClientApplication application(
            IHttpClient client, IClientSignedAssertionProvider provider, String authority)
            throws Exception {
        return ConfidentialClientApplication
                .builder("client",
                        ClientCredentialFactory.createFromSignedAssertionCallback(provider))
                .authority(authority)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(client)
                .build();
    }

    private static CompletableFuture<IAuthenticationResult> acquireOtherGrant(
            ConfidentialClientApplication app, String grant) throws Exception {
        switch (grant) {
            case "obo":
                return app.acquireToken(OnBehalfOfParameters.builder(
                                Collections.singleton(SCOPE), new UserAssertion(TestHelper.signedAssertion))
                        .skipCache(true).build());
            case "authorization_code":
                return app.acquireToken(AuthorizationCodeParameters.builder(
                        "code", new URI("http://localhost")).scopes(Collections.singleton(SCOPE)).build());
            case "refresh_token":
                return app.acquireToken(RefreshTokenParameters.builder(
                        Collections.singleton(SCOPE), "refresh-token").build());
            default:
                throw new IllegalArgumentException("Unsupported test grant: " + grant);
        }
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
