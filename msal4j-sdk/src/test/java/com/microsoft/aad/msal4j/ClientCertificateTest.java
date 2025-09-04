// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

            String clientAssertion = extractClientAssertion(requestBody);

            if (clientAssertion != null) {
                SignedJWT signedJWT = SignedJWT.parse(clientAssertion);
                if (signedJWT.getHeader().toJSONObject().containsKey("x5t#S256")) {
                    return TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(tokenResponseValues));
                }
            }

            //If the client assertion is null or does not contain the x5t#S256 header,
            // that indicates a problem in assertion generation and this test should fail.
            return null;
        });
        
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton("scopes")).build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();

        assertNotNull(result);
        assertEquals("accessTokenSha256", result.accessToken());
    }

    @Test
    void testClientCertificate_GeneratesNewAssertionEachTime() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        List<String> capturedAssertions = new ArrayList<>();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromCertificate(TestHelper.getPrivateKey(), TestHelper.getX509Cert()))
                        .authority("https://login.microsoftonline.com/tenant")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Mock the HTTP client to capture assertions from each request
        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(parameters -> {
            HttpRequest request = parameters.getArgument(0);
            String requestBody = request.body();

            String clientAssertion = extractClientAssertion(requestBody);
            if (clientAssertion != null) {
                capturedAssertions.add(clientAssertion);

                // Verify it's a valid JWT with proper headers
                SignedJWT signedJWT = SignedJWT.parse(clientAssertion);
                if (signedJWT.getHeader().toJSONObject().containsKey("x5t#S256")) {
                    HashMap<String, String> tokenResponseValues = new HashMap<>();
                    tokenResponseValues.put("access_token", "access_token_" + capturedAssertions.size());
                    return TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(tokenResponseValues));
                }
            }
            return null;
        });

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton("scopes")).skipCache(true).build();

        // Make two token requests
        IAuthenticationResult result1 = cca.acquireToken(parameters).get();
        IAuthenticationResult result2 = cca.acquireToken(parameters).get();

        // Verify two unique assertions were generated
        assertEquals(2, capturedAssertions.size(), "Two assertions should have been generated");
        assertNotEquals(capturedAssertions.get(0), capturedAssertions.get(1),
                "Each token request should generate a unique assertion");

        // Optional: Parse and verify JWT properties if needed
        SignedJWT jwt1 = SignedJWT.parse(capturedAssertions.get(0));
        SignedJWT jwt2 = SignedJWT.parse(capturedAssertions.get(1));

        // Different JTI (JWT ID) should be used for each assertion
        assertNotEquals(jwt1.getJWTClaimsSet().getJWTID(), jwt2.getJWTClaimsSet().getJWTID(),
                "Each assertion should have a unique JTI claim");

        // Verify the tokens are different
        assertNotEquals(result1.accessToken(), result2.accessToken(),
                "The access tokens from each request should be different");
    }

    /**
     * Extracts the client_assertion value from a URL-encoded request body
     * @param requestBody The request body string
     * @return The extracted client assertion or null if not found
     */
    private String extractClientAssertion(String requestBody) {
        try {
            // Split the request body into key-value pairs
            String[] pairs = requestBody.split("&");
            for (String pair : pairs) {
                // Find the client_assertion parameter
                if (pair.startsWith("client_assertion=")) {
                    // Extract and URL-decode the value
                    return URLDecoder.decode(pair.substring("client_assertion=".length()), StandardCharsets.UTF_8.toString());
                }
            }
        } catch (Exception e) {
            // In case of any parsing errors
            System.err.println("Error extracting client assertion: " + e.getMessage());
        }
        return null;
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
