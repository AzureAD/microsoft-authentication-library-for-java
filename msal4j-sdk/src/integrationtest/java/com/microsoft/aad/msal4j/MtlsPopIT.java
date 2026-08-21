// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.KeyVaultSecretsProvider;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;

import static com.microsoft.aad.msal4j.TestConstants.AGENTIC_GRAPH_SCOPE;
import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * End-to-end integration tests for SN/I certificate over mTLS Proof-of-Possession (PoP).
 *
 * <p>These exercise the primary deliverable of this work: a confidential-client app configured with a
 * Subject-Name/Issuer (SN/I) certificate obtains an <b>mTLS-bound PoP access token</b> from Entra ID
 * (ESTS), where that same SNI cert is presented as the client TLS certificate in the mutual-TLS
 * handshake to the token endpoint (no {@code private_key_jwt} / x5c client assertion on the direct
 * path).
 *
 * <p>The primary scenario is covered:
 * <ul>
 *   <li><b>Direct SNI cert &rarr; mTLS PoP</b> (client credentials), global and regional endpoints.</li>
 * </ul>
 *
 * <p><b>Testability gate (SME note A):</b> ESTS gates mTLS PoP on the <i>final resource audience</i>,
 * which must be an ESTS allow-listed resource (e.g. Azure Key Vault or MS Graph) — not the client app.
 * Every test below therefore requests a token for an allow-listed resource.
 *
 * <p>The lab SN/I certificate is <b>non-CNG</b>, so these tests are E2E-runnable in CI/CD using the same
 * certificate the pipelines already provision for {@code ClientCredentialsIT} and {@code AgenticIT} (the
 * OS keystore alias {@link KeyVaultSecretsProvider#CERTIFICATE_ALIAS}). They require lab credentials and
 * network access and only pass in CI (like the other {@code *IT} tests, they are not run by the unit-test
 * surefire pass).
 *
 * <p><b>App/tenant requirement:</b> ESTS only issues mTLS PoP tokens for the SN/I-allow-listed app in the
 * MSI team tenant. This mirrors MSAL .NET's {@code ClientCredentialsMtlsPopTests}: any other app (e.g. the
 * general {@code LabVaultAppID}) is rejected with {@code AADSTS700025: Client is public...}. The lab cert
 * ({@link KeyVaultSecretsProvider#CERTIFICATE_ALIAS}) is registered for SN/I on this app.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MtlsPopIT {

    // SN/I-allow-listed app and MSI team tenant. mTLS PoP only works on this app/tenant pair; mirrors
    // MSAL .NET's ClientCredentialsMtlsPopTests. App and tenant IDs are public identifiers, not secrets.
    private static final String SNI_ALLOWLISTED_APP_ID = "163ffef9-a313-45b4-ab2f-c7e2f5e0e23e";
    private static final String SNI_ALLOWLISTED_AUTHORITY =
            "https://login.microsoftonline.com/bea21ebe-8b64-4d06-9f6d-6a889b120a7c";
    // ESTS test-slice region. Used ONLY by the deterministic Bearer cell to exercise the regional
    // mtlsauth endpoint live; the mtls_pop cells stay global because this slice intermittently
    // downgrades the mtls_pop token_type (a downgrade must never be masked as a regional quirk).
    private static final String TEST_SLICE_REGION = "westus3";

    // mTLS-enabled MS Graph host (NOT plain graph.microsoft.com, which does not perform the client-cert
    // handshake). A token bound to the presented certificate is accepted here with HTTP 200.
    private static final String MTLS_GRAPH_RESOURCE =
            "https://mtlstb.graph.microsoft.com/v1.0/applications?$top=1";

    private PrivateKey privateKey;
    private X509Certificate publicCertificate;
    private IClientCertificate certificate;

    @BeforeAll
    void init() throws KeyStoreException, NoSuchProviderException, IOException,
            NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        KeyStore keystore = CertificateHelper.createKeyStore();
        keystore.load(null, null);

        privateKey = (PrivateKey) keystore.getKey(KeyVaultSecretsProvider.CERTIFICATE_ALIAS, null);
        publicCertificate = (X509Certificate) keystore.getCertificate(KeyVaultSecretsProvider.CERTIFICATE_ALIAS);

        // These are live-lab E2E tests (like the other *IT classes). Off-CI the lab SN/I cert is absent
        // from the OS keystore, so SKIP the whole class rather than hard-failing it: a missing lab cert
        // is an environment condition, not a product defect.
        Assumptions.assumeTrue(privateKey != null && publicCertificate != null,
                "Lab SN/I certificate not available (alias '" + KeyVaultSecretsProvider.CERTIFICATE_ALIAS
                        + "'); skipping mTLS PoP E2E. Expected off-CI.");

        certificate = ClientCredentialFactory.createFromCertificate(privateKey, publicCertificate);
    }

    /**
     * <b>X509 SNI cert &rarr; mTLS PoP, proven end to end.</b> Canonical matrix cell
     * {@code Credential_X509_Output_Pop}: with <b>no region</b> configured (global
     * {@code mtlsauth.microsoft.com} endpoint), the lab cert is presented as the client TLS certificate
     * and the request carries {@code token_type=mtls_pop} with <b>no</b> client assertion.
     *
     * <p>Beyond asserting the token is issued and bound to the cert, this proves the bound token is
     * <b>actually usable</b>: it is presented (with the binding cert on the TLS handshake) to an
     * mTLS-enabled resource, which must return HTTP 200. A 401/403 would mean the certificate was not
     * presented or the {@code mtls_pop} scheme was wrong. The MS Graph scope is used because Graph is an
     * ESTS mTLS-PoP allow-listed resource whose {@code mtlstb.graph.microsoft.com} host performs the
     * client-cert handshake (the app must be granted Graph {@code Application.Read.All}).
     */
    @Test
    void Credential_X509_Output_Pop() throws Exception {
        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(SNI_ALLOWLISTED_APP_ID, certificate)
                .authority(SNI_ALLOWLISTED_AUTHORITY)   // tenanted authority (required for mTLS PoP)
                .build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(AGENTIC_GRAPH_SCOPE))
                        .mtlsProofOfPossession()
                        .build())
                .get();

        assertMtlsPopResult(result, expectedLabThumbprint());

        MtlsResourceCaller.Response resourceResponse = MtlsResourceCaller.callResourceWithMtlsToken(
                MTLS_GRAPH_RESOURCE, result.accessToken(), certificate);
        assertEquals(200, resourceResponse.statusCode(),
                "mTLS-enabled resource must accept the bound PoP token (HTTP 200). A 401/403 means the "
                        + "binding certificate was not presented on the handshake or the mtls_pop scheme was "
                        + "wrong; a 400 means the request reached the resource but was rejected as malformed "
                        + "(e.g. token audience/shape). Status " + resourceResponse.statusCode()
                        + ", error body: " + resourceResponse.errorBody());
    }

    /**
     * Canonical matrix cell {@code Credential_X509_Output_Bearer}: the same SN/I cert <b>without</b>
     * {@code mtlsProofOfPossession()} yields the existing {@code Bearer} token (the cert signs a
     * {@code private_key_jwt} client assertion) and exposes <b>no</b> binding certificate. This anchors
     * that opting out of mTLS PoP leaves the legacy SNI+Bearer behaviour intact.
     */
    @Test
    void Credential_X509_Output_Bearer() throws Exception {
        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(SNI_ALLOWLISTED_APP_ID, certificate)
                .authority(SNI_ALLOWLISTED_AUTHORITY)
                .azureRegion(TEST_SLICE_REGION)   // regional endpoint is safe here (token type is deterministic)
                .build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .build())   // no mtlsProofOfPossession() -> Bearer
                .get();

        assertNotNull(result.accessToken(), "Access token should not be null");
        assertEquals(TokenType.BEARER, result.metadata().tokenType(), "Result token type should be BEARER");
        assertNull(result.metadata().bindingCertificate(),
                "Bearer result must not expose a binding certificate");
    }

    /**
     * {@code Credential_X509_Output_Pop} cache behavior: a bound mTLS-PoP token is cached under
     * {@code {token_type + cert KeyId}} and returned on a second acquisition for the same scope. Uses
     * the global {@code mtlsauth.microsoft.com} endpoint so the {@code mtls_pop} token type can be
     * asserted directly &mdash; the {@code westus3} test slice intermittently downgrades the token type
     * and is therefore never used for token-type assertions.
     */
    @Test
    void Credential_X509_Output_Pop_CacheHit() throws Exception {
        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(SNI_ALLOWLISTED_APP_ID, certificate)
                .authority(SNI_ALLOWLISTED_AUTHORITY)
                .build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .mtlsProofOfPossession()
                        .build())
                .get();

        assertMtlsPopResult(result, expectedLabThumbprint());

        // The mTLS-PoP token must be cached under {token_type + cert KeyId} and returned on lookup.
        IAuthenticationResult cached = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .mtlsProofOfPossession()
                        .build())
                .get();

        assertEquals(result.accessToken(), cached.accessToken(),
                "Second mTLS-PoP request should return the cached bound token");
    }

    /**
     * Requesting a Bearer token and an mTLS-PoP token for the same scope on the same app must yield two
     * distinct tokens (cache isolation on {token_type + cert KeyId}), confirming the PoP path never
     * aliases the existing SNI+Bearer path.
     */
    @Test
    void Credential_X509_Output_Pop_And_Bearer_CacheIsolated() throws Exception {
        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(SNI_ALLOWLISTED_APP_ID, certificate)
                .authority(SNI_ALLOWLISTED_AUTHORITY)
                .build();

        // Existing SNI + Bearer path (unchanged).
        IAuthenticationResult bearer = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .build())
                .get();
        assertEquals(TokenType.BEARER, bearer.metadata().tokenType());

        // New SNI + mTLS PoP path.
        IAuthenticationResult pop = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .mtlsProofOfPossession()
                        .build())
                .get();
        assertEquals(TokenType.MTLS_POP, pop.metadata().tokenType());

        assertNotEquals(bearer.accessToken(), pop.accessToken(),
                "Bearer and mTLS-PoP tokens for the same scope must be distinct cache entries");
        assertEquals(2, cca.tokenCache.accessTokens.size(),
                "Bearer and mTLS-PoP tokens must occupy separate cache entries");
    }

    private void assertMtlsPopResult(IAuthenticationResult result, String expectedThumbprint) {
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
        assertEquals(TokenType.MTLS_POP, result.metadata().tokenType(),
                "Result token type should be MTLS_POP");

        BindingCertificate binding = result.metadata().bindingCertificate();
        assertNotNull(binding, "mTLS-PoP result must expose a binding certificate");
        assertFalse(binding.certificateChain().isEmpty(), "Binding certificate must expose its x5c chain");
        assertEquals(expectedThumbprint, binding.thumbprintSha256(),
                "Binding certificate thumbprint must match the lab SNI cert (x5t#S256)");
    }

    private String expectedLabThumbprint() {
        return MtlsClientCertificateHelper.computeThumbprintSha256(publicCertificate);
    }
}
