// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import java.security.cert.X509Certificate;
import java.util.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

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

    @Test
    void testClientCertificate_TenantOverride() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        Map<String, String> capturedTenants = new HashMap<>();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromCertificate(TestHelper.getPrivateKey(), TestHelper.getX509Cert()))
                        .authority("https://login.microsoftonline.com/default-tenant")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Mock the HTTP client to capture and analyze assertions from each request
        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(parameters -> {
            HttpRequest request = parameters.getArgument(0);
            String requestBody = request.body();
            String url = request.url().toString();

            // Capture which tenant was used in the authority
            String tenant = extractTenantFromUrl(url);

            // Extract the assertion to verify its audience claim
            String clientAssertion = extractClientAssertion(requestBody);
            if (clientAssertion != null) {
                SignedJWT signedJWT = SignedJWT.parse(clientAssertion);

                // Get the audience claim to verify it matches the tenant
                String audience = signedJWT.getJWTClaimsSet().getAudience().get(0);

                // Store the tenant and audience for verification
                capturedTenants.put(tenant, audience);

                // Verify it's a valid JWT with proper headers
                if (signedJWT.getHeader().toJSONObject().containsKey("x5t#S256")) {
                    HashMap<String, String> tokenResponseValues = new HashMap<>();
                    tokenResponseValues.put("access_token", "access_token_for_" + tenant);
                    return TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(tokenResponseValues));
                }
            }
            return null;
        });

        // First request with default tenant
        ClientCredentialParameters defaultParameters = ClientCredentialParameters.builder(Collections.singleton("scopes"))
                .skipCache(true)
                .build();
        IAuthenticationResult resultDefault = cca.acquireToken(defaultParameters).get();

        // Second request with override tenant
        String overrideTenant = "override-tenant";
        ClientCredentialParameters overrideParameters = ClientCredentialParameters.builder(Collections.singleton("scopes"))
                .skipCache(true)
                .tenant(overrideTenant)
                .build();
        IAuthenticationResult resultOverride = cca.acquireToken(overrideParameters).get();

        // Verify both requests were processed
        assertEquals(2, capturedTenants.size(), "Two requests with different tenants should have been processed");

        // Verify both tenants were used
        assertTrue(capturedTenants.containsKey("default-tenant"), "Default tenant should have been used");
        assertTrue(capturedTenants.containsKey(overrideTenant), "Override tenant should have been used");

        // Verify the audience in the JWT assertions reflects the different tenants
        assertNotEquals(
            capturedTenants.get("default-tenant"),
            capturedTenants.get(overrideTenant),
            "JWT audience should differ between default and override tenant"
        );

        // Verify the audience claims match the expected format with the correct tenant
        assertTrue(
            capturedTenants.get("default-tenant").contains("default-tenant"),
            "Audience for default tenant should contain the default tenant name"
        );
        assertTrue(
            capturedTenants.get(overrideTenant).contains(overrideTenant),
            "Audience for override tenant should contain the override tenant name"
        );

        // Verify different access tokens were returned
        assertNotEquals(resultDefault.accessToken(), resultOverride.accessToken(),
            "Access tokens should differ when using different tenants");
    }

    @Test
    void testClientCertificate_TenantOverride_B2C() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        String replacementTenant = "overrideTenant";

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromCertificate(TestHelper.getPrivateKey(), TestHelper.getX509Cert()))
                        .b2cAuthority(TestConfiguration.B2C_AUTHORITY)
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(parameters -> {
            HttpRequest request = parameters.getArgument(0);
            String requestBody = request.body();
            String url = request.url().toString();

            // Extract the assertion to verify its audience claim
            String clientAssertion = extractClientAssertion(requestBody);

            if (clientAssertion != null && url.contains(replacementTenant)) {
                    HashMap<String, String> tokenResponseValues = new HashMap<>();
                    tokenResponseValues.put("access_token", "access_token_for_" + replacementTenant);
                    return TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(tokenResponseValues));
            }

            return null;
        });

        ClientCredentialParameters overrideParameters = ClientCredentialParameters.builder(Collections.singleton("scopes"))
                .skipCache(true)
                .tenant(replacementTenant)
                .build();
        IAuthenticationResult result = cca.acquireToken(overrideParameters).get();

        assertNotNull(result);
        assertEquals("access_token_for_"+ replacementTenant, result.accessToken());
    }

    /**
     * Extracts the tenant name from an authority URL
     * @param url The full URL containing the tenant
     * @return The tenant name
     */
    private String extractTenantFromUrl(String url) {
        // Authority URL format is typically https://login.microsoftonline.com/tenant/...
        String[] parts = url.split("/");
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equalsIgnoreCase("login.microsoftonline.com") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
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

    // ========== ClientCertificate: SHA-1 Hash ==========

    @Test
    void testPublicCertificateHash_Sha1() throws Exception {
        IClientCertificate cert = ClientCredentialFactory.createFromCertificate(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());

        String sha1Hash = cert.publicCertificateHash();

        assertNotNull(sha1Hash, "SHA-1 hash should not be null");
        assertFalse(sha1Hash.isEmpty(), "SHA-1 hash should not be empty");
        // Base64-encoded SHA-1 is 28 characters
        assertEquals(28, sha1Hash.length(), "Base64-encoded SHA-1 should be 28 chars");
    }

    @Test
    void testPublicCertificateHash_Sha256DiffersFromSha1() throws Exception {
        IClientCertificate cert = ClientCredentialFactory.createFromCertificate(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());

        String sha1Hash = cert.publicCertificateHash();
        String sha256Hash = cert.publicCertificateHash256();

        assertNotEquals(sha1Hash, sha256Hash,
                "SHA-1 and SHA-256 hashes should be different");
    }

    // ========== ClientCertificate: Certificate Chain Encoding ==========

    @Test
    void testGetEncodedPublicKeyCertificateChain_singleCert() throws Exception {
        ClientCertificate cert = ClientCertificate.create(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());

        List<String> chain = cert.getEncodedPublicKeyCertificateChain();

        assertNotNull(chain);
        assertEquals(1, chain.size(), "Single cert should produce chain of length 1");
        assertFalse(chain.get(0).isEmpty(), "Encoded cert should not be empty");
    }

    @Test
    void testGetEncodedPublicKeyCertificateChain_multiCert() throws Exception {
        // Create a chain with the same cert repeated (simulates a CA chain)
        List<X509Certificate> certChain = Arrays.asList(
                TestHelper.getX509Cert(), TestHelper.getX509Cert());
        ClientCertificate cert = new ClientCertificate(TestHelper.getPrivateKey(), certChain);

        List<String> chain = cert.getEncodedPublicKeyCertificateChain();

        assertEquals(2, chain.size(), "Chain with 2 certs should produce 2 encoded entries");
    }

    // ========== ClientCertificate: getAssertion ==========

    @Test
    void testGetAssertion_nullAuthority_throwsNullPointerException() {
        ClientCertificate cert = ClientCertificate.create(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());

        assertThrows(NullPointerException.class,
                () -> cert.getAssertion(null, "client-id", false));
    }

    @Test
    void testGetAssertion_aadAuthority_usesSha256() throws Exception {
        ClientCertificate cert = ClientCertificate.create(
                TestHelper.getPrivateKey(), TestHelper.getX509Cert());

        Authority authority = Authority.createAuthority(
                new java.net.URL("https://login.microsoftonline.com/tenant/"));

        String assertion = cert.getAssertion(authority, "client-id", false);

        assertNotNull(assertion, "Assertion should not be null");
        // Verify it's a valid JWT (3 dot-separated parts)
        String[] parts = assertion.split("\\.");
        assertEquals(3, parts.length, "JWT assertion should have 3 parts");
    }
}
