// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BearerOverMtlsTest {
    private static final String AUTHORITY = "https://login.microsoftonline.com/tenant/";
    private static final String SCOPE = "https://graph.microsoft.com/.default";

    @Test
    void defaultsOffAndRequiresCertificateAtBuild() throws Exception {
        ConfidentialClientApplication ordinary = ConfidentialClientApplication
                .builder("client", ClientCredentialFactory.createFromSecret("secret"))
                .build();
        assertFalse(ordinary.sendCertificateOverMtls());

        assertThrows(MsalClientException.class, () -> ConfidentialClientApplication
                .builder("client", ClientCredentialFactory.createFromSecret("secret"))
                .sendCertificateOverMtls(true)
                .build());
    }

    @Test
    void sendsVerifiableAssertionAndReturnsCachedBearerWithoutBinding() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("token-a"));
        ConfidentialClientApplication app = application(client, providerBackedCertificate())
                .sendCertificateOverMtls(true)
                .sendX5c(false)
                .build();

        IAuthenticationResult first = app.acquireToken(clientCredentialParameters(false)).get();
        IAuthenticationResult cached = app.acquireToken(clientCredentialParameters(false)).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(1)).send(request.capture());
        assertEquals("mtlsauth.microsoft.com", request.getValue().url().getHost());
        assertFalse(request.getValue().followRedirects());
        assertNotNull(request.getValue().sslContext());
        assertFalse(request.getValue().body().contains("token_type=mtls_pop"));
        assertFalse(request.getValue().body().contains("req_cnf"));

        SignedJWT assertion = SignedJWT.parse(parameter(request.getValue().body(), "client_assertion"));
        assertEquals("RS256", assertion.getHeader().getAlgorithm().getName());
        assertNotNull(assertion.getHeader().getX509CertChain());
        assertNotNull(assertion.getHeader().getX509CertSHA256Thumbprint());
        assertEquals("client", assertion.getJWTClaimsSet().getIssuer());
        assertEquals("client", assertion.getJWTClaimsSet().getSubject());
        assertTrue(assertion.verify(new RSASSAVerifier(
                (RSAPublicKey) TestHelper.getX509Cert().getPublicKey())));

        assertEquals("Bearer", first.tokenType());
        assertNull(first.bindingCertificate());
        assertNull(first.mtlsBindingContext());
        assertEquals(TokenSource.CACHE, cached.metadata().tokenSource());
        assertEquals(first.accessToken(), cached.accessToken());
    }

    @Test
    void popTakesPrecedence() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("pop", "mtls_pop"));
        ConfidentialClientApplication app = application(client, certificate())
                .sendCertificateOverMtls(true)
                .build();

        IAuthenticationResult result = app.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(SCOPE))
                .mtlsProofOfPossession()
                .skipCache(true)
                .build()).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture());
        assertTrue(request.getValue().body().contains("token_type=mtls_pop"));
        assertFalse(request.getValue().body().contains("client_assertion"));
        assertNotNull(result.mtlsBindingContext());
    }

    @Test
    void requestCredentialOverrideDrivesTlsAndAssertionSnapshot() throws Exception {
        KeyPair applicationKeyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        IClientCertificate applicationCertificate =
                invalidApplicationCertificate(applicationKeyPair.getPrivate());
        IClientCertificate overrideCertificate = certificate();
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("override"));
        ConfidentialClientApplication app = application(client, applicationCertificate)
                .sendCertificateOverMtls(true)
                .build();

        app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .clientCredential(overrideCertificate)
                .skipCache(true)
                .build()).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture());
        assertNotNull(request.getValue().sslContext());
        SignedJWT assertion = SignedJWT.parse(
                parameter(request.getValue().body(), "client_assertion"));
        assertTrue(assertion.verify(new RSASSAVerifier(
                (RSAPublicKey) TestHelper.getX509Cert().getPublicKey())));
        assertFalse(assertion.verify(new RSASSAVerifier(
                (RSAPublicKey) applicationKeyPair.getPublic())));

        List<String> assertionChain = new ArrayList<>();
        assertion.getHeader().getX509CertChain()
                .forEach(value -> assertionChain.add(value.toString()));
        assertEquals(overrideCertificate.getEncodedPublicKeyCertificateChain(),
                assertionChain);
        assertNotEquals("AQID",
                assertion.getHeader().getX509CertChain().get(0).toString());
        assertNull(assertion.getHeader().getX509CertThumbprint());
        assertEquals(Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-256")
                                .digest(TestHelper.getX509Cert().getEncoded())),
                assertion.getHeader().getX509CertSHA256Thumbprint().toString());
    }

    @Test
    void tenantOverrideUsesSameTenantForEndpointAndAssertionAudience() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("tenant"));
        ConfidentialClientApplication app = application(client, certificate())
                .sendCertificateOverMtls(true)
                .build();

        app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .tenant("override-tenant")
                .skipCache(true)
                .build()).get();

        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client).send(request.capture());
        assertEquals("https://mtlsauth.microsoft.com/override-tenant/oauth2/v2.0/token",
                request.getValue().url().toString());
        SignedJWT assertion = SignedJWT.parse(
                parameter(request.getValue().body(), "client_assertion"));
        assertEquals(Collections.singletonList(
                        "https://login.microsoftonline.com/override-tenant/oauth2/v2.0/token"),
                assertion.getJWTClaimsSet().getAudience());
    }

    @Test
    void acceptsBearerCaseVariants() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("lower", "bearer"))
                .thenReturn(response("upper", "BEARER"));
        ConfidentialClientApplication app = application(client, certificate())
                .sendCertificateOverMtls(true)
                .build();

        assertEquals("lower", app.acquireToken(clientCredentialParameters(true)).get().accessToken());
        assertEquals("upper", app.acquireToken(clientCredentialParameters(true)).get().accessToken());
    }

    @Test
    void unexpectedTokenTypeFailsClosedAndIsNotCached() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class)))
                .thenReturn(response("unexpected", "mtls_pop"))
                .thenReturn(response("bearer"));
        ConfidentialClientApplication app = application(client, certificate())
                .sendCertificateOverMtls(true)
                .build();

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(clientCredentialParameters(false)).get());
        assertTrue(failure.getCause() instanceof MsalClientException);
        assertEquals(AuthenticationErrorCode.TOKEN_TYPE_MISMATCH,
                ((MsalClientException) failure.getCause()).errorCode());
        assertEquals("bearer",
                app.acquireToken(clientCredentialParameters(false)).get().accessToken());
        verify(client, times(2)).send(any(HttpRequest.class));
    }

    @Test
    void rejectsBearerOverMtlsProtocolOverridesButOrdinaryPathRemainsCompatible()
            throws Exception {
        for (String name : new String[]{"TOKEN_TYPE", "req_cnf"}) {
            DefaultHttpClient rejectedClient = mock(DefaultHttpClient.class);
            ConfidentialClientApplication rejectedApp =
                    application(rejectedClient, certificate())
                            .sendCertificateOverMtls(true)
                            .build();
            ClientCredentialParameters parameters = ClientCredentialParameters
                    .builder(Collections.singleton(SCOPE))
                    .extraQueryParameters(Collections.singletonMap(name, "value"))
                    .skipCache(true)
                    .build();

            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> rejectedApp.acquireToken(parameters).get());
            assertTrue(failure.getCause() instanceof MsalClientException);
            assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR,
                    ((MsalClientException) failure.getCause()).errorCode());
            verify(rejectedClient, times(0)).send(any(HttpRequest.class));
        }

        DefaultHttpClient ordinaryClient = mock(DefaultHttpClient.class);
        when(ordinaryClient.send(any(HttpRequest.class))).thenReturn(response("ordinary"));
        ConfidentialClientApplication ordinaryApp =
                application(ordinaryClient, certificate()).build();
        ordinaryApp.acquireToken(ClientCredentialParameters.builder(
                        Collections.singleton(SCOPE))
                .extraQueryParameters(Collections.singletonMap("token_type", "custom"))
                .skipCache(true)
                .build()).get();
        ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
        verify(ordinaryClient).send(request.capture());
        assertTrue(request.getValue().body().contains("token_type=custom"));
    }

    @Test
    void allSupportedConfidentialGrantShapesUseBearerOverMtls() throws Exception {
        DefaultHttpClient client = mock(DefaultHttpClient.class);
        when(client.send(any(HttpRequest.class))).thenReturn(response("token"));
        ConfidentialClientApplication app = application(client, certificate())
                .sendCertificateOverMtls(true)
                .build();

        app.acquireToken(ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .skipCache(true).build()).get();
        app.acquireToken(OnBehalfOfParameters.builder(Collections.singleton(SCOPE),
                new UserAssertion("user-assertion")).skipCache(true).build()).get();
        app.acquireToken(AuthorizationCodeParameters.builder("code", new URI("http://localhost"))
                .scopes(Collections.singleton(SCOPE)).build()).get();
        app.acquireToken(RefreshTokenParameters.builder(Collections.singleton(SCOPE), "refresh").build()).get();
        app.acquireToken(UserFederatedIdentityCredentialParameters.builder(
                Collections.singleton(SCOPE), "user@contoso.com", "fic").forceRefresh(true).build()).get();

        ArgumentCaptor<HttpRequest> requests = ArgumentCaptor.forClass(HttpRequest.class);
        verify(client, times(5)).send(requests.capture());
        List<HttpRequest> values = requests.getAllValues();
        for (HttpRequest request : values) {
            assertEquals("mtlsauth.microsoft.com", request.url().getHost());
            assertTrue(request.body().contains("client_assertion="));
            assertFalse(request.body().contains("token_type=mtls_pop"));
        }
        assertTrue(values.get(0).body().contains("grant_type=client_credentials"));
        assertTrue(values.get(1).body().contains("requested_token_use=on_behalf_of"));
        assertTrue(values.get(2).body().contains("grant_type=authorization_code"));
        assertTrue(values.get(3).body().contains("grant_type=refresh_token"));
        assertTrue(values.get(4).body().contains("grant_type=user_fic"));
    }

    @Test
    void incompatibleCustomClientFailsClosed() throws Exception {
        IHttpClient client = mock(IHttpClient.class);
        ConfidentialClientApplication app = ConfidentialClientApplication
                .builder("client", certificate())
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(client)
                .sendCertificateOverMtls(true)
                .build();

        ExecutionException failure = assertThrows(ExecutionException.class,
                () -> app.acquireToken(clientCredentialParameters(true)).get());
        assertTrue(failure.getCause() instanceof MsalClientException);
        assertEquals(AuthenticationErrorCode.MTLS_POP_ERROR,
                ((MsalClientException) failure.getCause()).errorCode());
        verify(client, times(0)).send(any(HttpRequest.class));
    }

    private static ConfidentialClientApplication.Builder application(
            IHttpClient client, IClientCertificate certificate) throws Exception {
        return ConfidentialClientApplication.builder("client", certificate)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .httpClient(client);
    }

    private static ClientCredentialParameters clientCredentialParameters(boolean skipCache) {
        return ClientCredentialParameters.builder(Collections.singleton(SCOPE))
                .skipCache(skipCache)
                .build();
    }

    private static IClientCertificate certificate() {
        return ClientCertificate.create(TestHelper.getPrivateKey(), TestHelper.getX509Cert());
    }

    private static IClientCertificate providerBackedCertificate() {
        assertNull(TestHelper.getPrivateKey().getEncoded());
        return ClientCredentialFactory.createFromCertificate(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());
    }

    private static IClientCertificate invalidApplicationCertificate(PrivateKey privateKey) {
        return new IClientCertificate() {
            @Override
            public PrivateKey privateKey() {
                return privateKey;
            }

            @Override
            public String publicCertificateHash()
                    throws CertificateEncodingException, NoSuchAlgorithmException {
                return "application-sha1";
            }

            @Override
            public String publicCertificateHash256()
                    throws CertificateEncodingException, NoSuchAlgorithmException {
                return "application-sha256";
            }

            @Override
            public List<String> getEncodedPublicKeyCertificateChain() {
                return Collections.singletonList("AQID");
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

    private static HttpResponse response(String accessToken) {
        return response(accessToken, "Bearer");
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
