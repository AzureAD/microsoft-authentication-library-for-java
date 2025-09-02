// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.PrivateKeyJWT;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.text.ParseException;
import java.util.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCertificateTest {

    @Test
    void testNullKey() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ClientCertificate.create((PrivateKey) null, null));

        assertEquals("PrivateKey is null or empty", ex.getMessage());
    }

    @Test
    void testGetClient() {
        final RSAPrivateKey key = mock(RSAPrivateKey.class);
        final BigInteger modulus = mock(BigInteger.class);
        doReturn(2048).when(modulus).bitLength();
        doReturn(modulus).when(key).getModulus();

        final ClientCertificate kc = ClientCertificate.create(key, null);
        assertNotNull(kc);
    }

    @Test
    void testIClientCertificateInterface_Sha1andSha256() throws NoSuchAlgorithmException, CertificateException {
        //See https://github.com/AzureAD/microsoft-authentication-library-for-java/issues/863 for context on this test.
        //Essentially, it aims to test compatibility for customers that implemented IClientCertificate in older versions of the library.

        //IClientCertificate.publicCertificateHash256() returns null by default if not implemented...
        IClientCertificate certificate = new TestClientCredential();
        assertNull(certificate.publicCertificateHash256());

        //... but ClientCredentialFactory has an implemented version, so it should not be null.
        certificate = ClientCredentialFactory.createFromCertificate(TestHelper.getPrivateKey(), TestHelper.getX509Cert());
        assertNotNull(certificate.publicCertificateHash256());
    }

    @Test
    void testIClientCertificateInterface_CredentialFactoryUsesSha256() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromCertificate(TestHelper.getPrivateKey(), TestHelper.getX509Cert()))
                        .authority("https://login.microsoftonline.com/tenant")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        HashMap<String, String> tokenResponseValues = new HashMap<>();
        tokenResponseValues.put("access_token", "accessTokenSha256");

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(parameters -> {
            HttpRequest request = parameters.getArgument(0);
            String requestBody = request.body();

            SignedJWT signedJWT = SignedJWT.parse(cca.getAssertionString());

            if (requestBody.contains(cca.getAssertionString())
                    && signedJWT.getHeader().toJSONObject().containsKey("x5t#S256")) {
                return TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(tokenResponseValues));
            }
            return null;
        });
        
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton("scopes")).build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();

        assertNotNull(result);
        assertEquals("accessTokenSha256", result.accessToken());
    }

    class TestClientCredential implements IClientCertificate {
        @Override
        public PrivateKey privateKey() {
            return null;
        }

        @Override
        public String publicCertificateHash() {
            return "";
        }

        @Override
        public List<String> getEncodedPublicKeyCertificateChain() {
            return Collections.emptyList();
        }
    }
}
