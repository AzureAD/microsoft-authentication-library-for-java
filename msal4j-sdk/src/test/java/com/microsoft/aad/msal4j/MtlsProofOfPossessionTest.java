// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLContext;

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

class MtlsProofOfPossessionTest {
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant/";
    private static final String SCOPE = "https://graph.microsoft.com/.default";
    @Test
    void requestShapeTransportResultAndCacheHitUseOneBindingGeneration() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("token-a", "mtls_pop"));
        ConfidentialClientApplication app = application(client);

        IAuthenticationResult first = app.acquireToken(parameters(false)).get();
        IAuthenticationResult cached = app.acquireToken(parameters(false)).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(1)).send(request.capture());
        assertEquals("mtlsauth.microsoft.com", request.getValue().url().getHost());
        assertFalse(request.getValue().followRedirects());
        assertNotNull(request.getValue().sslContext());
        assertTrue(request.getValue().body().contains("token_type=mtls_pop"));
        assertFalse(request.getValue().body().contains("client_assertion"));
        assertFalse(request.getValue().body().contains("req_cnf"));

        assertEquals("mtls_pop", first.tokenType());
        assertNotNull(first.bindingCertificate());
        assertNotNull(first.mtlsBindingContext());
        assertNotNull(first.mtlsBindingContext().sslContext());
        assertEquals(first.bindingCertificate().thumbprintSha256(),
                first.mtlsBindingContext().keyId());
        assertTrue(first.expiresOnDate().getTime()
                <= first.mtlsBindingContext().notAfter().getTime());
        assertEquals("mtls_pop", cached.tokenType());
        assertNotNull(cached.mtlsBindingContext());
        assertEquals(first.mtlsBindingContext().keyId(), cached.mtlsBindingContext().keyId());
    }

    @Test
    void downgradeFailsClosedAndIsNotCached() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("downgrade", "Bearer"))
                .thenReturn(response("token-b", "mtls_pop"));
        ConfidentialClientApplication app = application(client);

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(parameters(false)).get());
        assertEquals(AuthenticationErrorCode.TOKEN_TYPE_MISMATCH,
                ((MsalClientException) failure.getCause()).errorCode());
        assertEquals("token-b", app.acquireToken(parameters(false)).get().accessToken());
        verify(client, times(2)).send(any(HttpRequest.class));
    }

    @Test
    void cacheIsAbaIsolatedAndRenewalWithSamePrivateKeyDifferentDerMisses() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("token-a", "mtls_pop"))
                .thenReturn(response("token-b", "mtls_pop"));
        ConfidentialClientApplication app = application(client);

        IAuthenticationResult a1 = app.acquireToken(parameters(false)).get();
        ClientCredentialParameters renewed = ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .clientCredential(renewedCertificateWithSamePrivateKey())
                .mtlsProofOfPossession()
                .build();
        IAuthenticationResult b = app.acquireToken(renewed).get();
        IAuthenticationResult a2 = app.acquireToken(parameters(false)).get();

        assertEquals("token-a", a1.accessToken());
        assertEquals("token-b", b.accessToken());
        assertEquals("token-a", a2.accessToken());
        assertFalse(a1.bindingCertificate().thumbprintSha256()
                .equals(b.bindingCertificate().thumbprintSha256()));
        verify(client, times(2)).send(any(HttpRequest.class));
    }

    @Test
    void rejectsCustomClientProtocolOverridesAndAppTokenProviderBeforeNetwork() throws Exception {
        IHttpClient incompatible = mock(IHttpClient.class);
        ConfidentialClientApplication customClientApp = ConfidentialClientApplication
                .builder("client", certificate())
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(incompatible)
                .build();
        assertThrows(ExecutionException.class,
                () -> customClientApp.acquireToken(parameters(true)).get());
        verify(incompatible, times(0)).send(any(HttpRequest.class));

        DefaultHttpClient compatible = mock(DefaultHttpClient.class);
        ConfidentialClientApplication providerApp = ConfidentialClientApplication
                .builder("client", certificate())
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(compatible)
                .appTokenProvider(ignored -> null)
                .build();
        assertThrows(ExecutionException.class,
                () -> providerApp.acquireToken(parameters(true)).get());
        verify(compatible, times(0)).send(any(HttpRequest.class));

        ClientCredentialParameters overridden = ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .mtlsProofOfPossession()
                .extraQueryParameters(Collections.singletonMap("TOKEN_TYPE", "Bearer"))
                .build();
        assertThrows(ExecutionException.class,
                () -> application(compatible).acquireToken(overridden).get());
        verify(compatible, times(0)).send(any(HttpRequest.class));
    }

    @Test
    void configuredApplicationTrustIsRejectedRatherThanSilentlyDiscarded() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder("client", certificate())
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .sslSocketFactory(SSLContext.getDefault().getSocketFactory())
                .build();

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(parameters(true)).get());
        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR,
                ((MsalClientException) failure.getCause()).errorCode());
    }

    @Test
    void ordinaryCertificateBearerPathIsUnchanged() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("bearer", "Bearer"));
        IAuthenticationResult result = application(client).acquireToken(
                ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                        .skipCache(true)
                        .build()).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture());
        assertEquals("login.microsoftonline.com", request.getValue().url().getHost());
        assertTrue(request.getValue().followRedirects());
        assertNull(request.getValue().sslContext());
        assertTrue(request.getValue().body().contains("client_assertion"));
        assertFalse(request.getValue().body().contains("token_type=mtls_pop"));
        assertEquals("Bearer", result.tokenType());
        assertNull(result.mtlsBindingContext());
    }

    private static ConfidentialClientApplication application(IHttpClient client) throws Exception {
        return ConfidentialClientApplication.builder("client", certificate())
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(client)
                .build();
    }

    private static IClientCertificate certificate() {
        return ClientCertificate.create(TestHelper.getPrivateKey(), TestHelper.getX509Cert());
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
            public java.util.List<String> getEncodedPublicKeyCertificateChain()
                    throws CertificateEncodingException {
                byte[] renewedDer = TestHelper.getX509Cert().getEncoded().clone();
                renewedDer[renewedDer.length - 1] ^= 1;
                return Collections.singletonList(
                        java.util.Base64.getEncoder().encodeToString(renewedDer));
            }
        };
    }

    private static ClientCredentialParameters parameters(boolean skipCache) {
        return ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .mtlsProofOfPossession()
                .skipCache(skipCache)
                .build();
    }

    private static HttpResponse response(String accessToken, String tokenType) {
        String body = "{\"access_token\":\"" + accessToken + "\",\"expires_in\":3600,"
                + "\"token_type\":\"" + tokenType + "\"}";
        HttpResponse response = new HttpResponse();
        response.statusCode(HttpStatus.HTTP_OK);
        response.body(body);
        response.addHeaders(Collections.singletonMap(
                "Content-Type", Collections.singletonList("application/json")));
        return response;
    }
}
