// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import javax.net.ssl.SSLSocketFactory;
import java.io.InputStream;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for mTLS Proof-of-Possession implementation:
 *  - MtlsPopAuthenticationScheme
 *  - MtlsSslContextHelper
 *  - ClientCredentialParameters.withMtlsProofOfPossession()
 *  - ManagedIdentityParameters.withMtlsProofOfPossession()
 *  - AccessTokenCacheEntity cache key isolation (keyId / AccessToken_With_AuthScheme)
 *  - CredentialTypeEnum.ACCESS_TOKEN_WITH_AUTH_SCHEME
 *  - AuthenticationResult tokenType / bindingCertificate
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MtlsPopTest {

    private static X509Certificate testCert;
    private static PrivateKey testKey;

    @BeforeAll
    static void loadTestCertificate() throws Exception {
        // Load the pre-generated test PKCS12 from test resources
        try (InputStream is = MtlsPopTest.class.getResourceAsStream("/mtls-test-cert.p12")) {
            assertNotNull(is, "mtls-test-cert.p12 must exist in test/resources");
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(is, "changeit".toCharArray());
            String alias = ks.aliases().nextElement();
            testKey = (PrivateKey) ks.getKey(alias, "changeit".toCharArray());
            testCert = (X509Certificate) ks.getCertificate(alias);
        }
    }

    // ─── MtlsPopAuthenticationScheme ────────────────────────────────────────

    @Test
    void scheme_constants() {
        assertEquals("mtls_pop", MtlsPopAuthenticationScheme.TOKEN_TYPE_MTLS_POP);
    }

    @Test
    void buildMtlsTokenEndpoint_publicCloud() throws Exception {
        String endpoint = MtlsPopAuthenticationScheme.buildMtlsTokenEndpoint(
                "eastus", "mytenant", "login.microsoftonline.com");
        assertEquals("https://eastus.mtlsauth.microsoft.com/mytenant/oauth2/v2.0/token", endpoint);
    }

    @Test
    void buildMtlsTokenEndpoint_publicCloud_noRegion() throws Exception {
        String endpoint = MtlsPopAuthenticationScheme.buildMtlsTokenEndpoint(
                null, "mytenant", "login.microsoftonline.com");
        assertEquals("https://mtlsauth.microsoft.com/mytenant/oauth2/v2.0/token", endpoint);
    }

    @Test
    void buildMtlsTokenEndpoint_sovereignCloud() throws Exception {
        String endpoint = MtlsPopAuthenticationScheme.buildMtlsTokenEndpoint(
                "eastus", "mytenant", "login.microsoftonline.us");
        assertEquals("https://eastus.mtlsauth.microsoftonline.us/mytenant/oauth2/v2.0/token", endpoint);
    }

    @Test
    void buildMtlsTokenEndpoint_usGov_throws() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsPopAuthenticationScheme.buildMtlsTokenEndpoint(
                        "eastus", "mytenant", "login.usgovcloudapi.net"));
        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void buildMtlsTokenEndpoint_china_throws() {
        MsalClientException ex = assertThrows(MsalClientException.class, () ->
                MtlsPopAuthenticationScheme.buildMtlsTokenEndpoint(
                        "eastus", "mytenant", "login.chinacloudapi.cn"));
        assertTrue(ex.getMessage().contains("not supported"));
    }

    @Test
    void computeX5tS256_producesBase64UrlNoPadding() throws Exception {
        String thumbprint = MtlsPopAuthenticationScheme.computeX5tS256(testCert);
        assertNotNull(thumbprint);
        assertFalse(thumbprint.isEmpty());
        assertFalse(thumbprint.contains("+"), "x5t#S256 must use Base64URL encoding (no +)");
        assertFalse(thumbprint.contains("/"), "x5t#S256 must use Base64URL encoding (no /)");
        assertFalse(thumbprint.contains("="), "x5t#S256 must not have padding");
    }

    @Test
    void computeX5tS256_deterministicForSameCert() throws Exception {
        String t1 = MtlsPopAuthenticationScheme.computeX5tS256(testCert);
        String t2 = MtlsPopAuthenticationScheme.computeX5tS256(testCert);
        assertEquals(t1, t2);
    }

    // ─── MtlsSslContextHelper ───────────────────────────────────────────────

    @Test
    void sslContextHelper_createsSslSocketFactory() throws Exception {
        SSLSocketFactory factory = MtlsSslContextHelper.createSslSocketFactory(
                testKey, new X509Certificate[]{testCert});
        assertNotNull(factory);
    }

    @Test
    void sslContextHelper_nullKey_throws() {
        assertThrows(Exception.class, () ->
                MtlsSslContextHelper.createSslSocketFactory(null, new X509Certificate[]{testCert}));
    }

    @Test
    void sslContextHelper_nullCertChain_throws() {
        assertThrows(Exception.class, () ->
                MtlsSslContextHelper.createSslSocketFactory(testKey, null));
    }

    // ─── ClientCredentialParameters ─────────────────────────────────────────

    @Test
    void clientCredentialParameters_mtlsPopDefault_false() {
        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("https://management.azure.com/.default"))
                .build();
        assertFalse(params.mtlsProofOfPossession());
    }

    @Test
    void clientCredentialParameters_withMtlsProofOfPossession_true() {
        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("https://management.azure.com/.default"))
                .withMtlsProofOfPossession()
                .build();
        assertTrue(params.mtlsProofOfPossession());
    }

    // ─── ManagedIdentityParameters ──────────────────────────────────────────

    @Test
    void managedIdentityParameters_mtlsPopDefault_false() {
        ManagedIdentityParameters params = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .build();
        assertFalse(params.mtlsProofOfPossession());
    }

    @Test
    void managedIdentityParameters_withMtlsProofOfPossession_true() {
        ManagedIdentityParameters params = ManagedIdentityParameters
                .builder("https://management.azure.com")
                .withMtlsProofOfPossession()
                .build();
        assertTrue(params.mtlsProofOfPossession());
    }

    // ─── CredentialTypeEnum ──────────────────────────────────────────────────

    @Test
    void credentialTypeEnum_accessTokenWithAuthScheme_value() {
        assertEquals("AccessToken_With_AuthScheme",
                CredentialTypeEnum.ACCESS_TOKEN_WITH_AUTH_SCHEME.value());
    }

    // ─── AccessTokenCacheEntity ──────────────────────────────────────────────

    @Test
    void cacheEntity_standardToken_keyHasNoKeyId() {
        AccessTokenCacheEntity entity = buildCacheEntity("AccessToken", null);
        String key = entity.getKey();
        // Standard Bearer token cache key: 6 parts separated by 5 dashes
        assertEquals(5, countOccurrences(key, '-'));
    }

    @Test
    void cacheEntity_mtlsPopToken_keyIncludesKeyId() throws Exception {
        String thumbprint = MtlsPopAuthenticationScheme.computeX5tS256(testCert);

        AccessTokenCacheEntity entity = buildCacheEntity(
                CredentialTypeEnum.ACCESS_TOKEN_WITH_AUTH_SCHEME.value(), thumbprint);
        String key = entity.getKey();
        assertTrue(key.contains(thumbprint.toLowerCase()),
                "Cache key for mTLS PoP token must include the thumbprint");
        // Key must have more segments than a standard Bearer token key (which has 6 parts / 5 dashes)
        assertTrue(key.endsWith("-" + thumbprint.toLowerCase()),
                "Thumbprint must be the last segment of the mTLS PoP cache key");
    }

    @Test
    void cacheEntity_mtlsPopAndBearerTokens_haveDifferentKeys() throws Exception {
        String thumbprint = MtlsPopAuthenticationScheme.computeX5tS256(testCert);

        AccessTokenCacheEntity bearer = buildCacheEntity(CredentialTypeEnum.ACCESS_TOKEN.value(), null);
        AccessTokenCacheEntity mtlsPop = buildCacheEntity(
                CredentialTypeEnum.ACCESS_TOKEN_WITH_AUTH_SCHEME.value(), thumbprint);

        assertNotEquals(bearer.getKey(), mtlsPop.getKey(),
                "Bearer and mTLS PoP tokens for the same scope must have different cache keys");
    }

    // ─── AuthenticationResult tokenType / bindingCertificate ────────────────

    @Test
    void authResult_defaultTokenType_null() {
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken("token")
                .build();
        assertNull(result.tokenType());
        assertNull(result.bindingCertificate());
    }

    @Test
    void authResult_mtlsPopFields_set() throws Exception {
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken("mtls_pop_token")
                .tokenType("mtls_pop")
                .bindingCertificate(testCert)
                .build();

        assertEquals("mtls_pop", result.tokenType());
        assertEquals(testCert, result.bindingCertificate());
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private static AccessTokenCacheEntity buildCacheEntity(String credentialType, String keyId) {
        AccessTokenCacheEntity entity = new AccessTokenCacheEntity();
        entity.homeAccountId("");
        entity.environment("login.microsoftonline.com");
        entity.credentialType(credentialType);
        entity.clientId("clientid");
        entity.realm("tenant");
        entity.target("scope");
        if (keyId != null) {
            entity.keyId(keyId);
        }
        return entity;
    }

    private static int countOccurrences(String s, char c) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == c) count++;
        }
        return count;
    }
}
