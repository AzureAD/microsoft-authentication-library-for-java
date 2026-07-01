// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.KeyVaultRegistry;
import com.microsoft.aad.msal4j.labapi.KeyVaultSecretsProvider;
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

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * End-to-end integration tests for SN/I certificate over mTLS Proof-of-Possession (PoP).
 *
 * <p>These exercise the primary deliverable of this work: a confidential-client app configured with a
 * Subject-Name/Issuer (SN/I) certificate obtains an <b>mTLS-bound PoP access token</b> from Entra ID
 * (ESTS), where that same SNI cert is presented as the client TLS certificate in the mutual-TLS
 * handshake to the token endpoint (no {@code private_key_jwt} / x5c client assertion on the direct
 * path).
 *
 * <p>Both scenarios from the plan are covered:
 * <ul>
 *   <li><b>Direct SNI cert &rarr; mTLS PoP</b> (client credentials), global and regional endpoints.</li>
 *   <li><b>2-leg FIC over mTLS PoP</b> — both legs are mTLS-PoP requests and the final token is bound to
 *       the Leg-1 certificate thumbprint.</li>
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
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MtlsPopIT {

    private static final String AGENTIC_AUTHORITY =
            "https://login.microsoftonline.com/" + TestConstants.AGENTIC_TENANT_ID + "/";

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

        assertNotNull(privateKey, "Lab private key not found. Ensure the lab cert is installed.");
        assertNotNull(publicCertificate, "Lab certificate not found. Ensure the lab cert is installed.");

        certificate = ClientCredentialFactory.createFromCertificate(privateKey, publicCertificate);
    }

    /**
     * Direct SNI cert &rarr; mTLS PoP with <b>no region</b> (exercises the global
     * {@code mtlsauth.microsoft.com} endpoint). The lab cert is presented as the client TLS certificate;
     * the request carries {@code token_type=mtls_pop} and <b>no</b> client assertion. Requests an
     * allow-listed resource (Key Vault) so ESTS issues the bound token.
     */
    @Test
    void acquireTokenClientCredentials_Certificate_MtlsPop() throws Exception {
        final String clientId = KeyVaultRegistry.getMsidLabProvider()
                .getSecretByName("LabVaultAppID").getValue();

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(clientId, certificate)
                .authority(TestConstants.MICROSOFT_AUTHORITY)   // tenanted authority (required for mTLS PoP)
                .build();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                        .mtlsProofOfPossession()
                        .build())
                .get();

        assertMtlsPopResult(result, expectedLabThumbprint());
    }

    /**
     * Direct SNI cert &rarr; mTLS PoP with a region configured (exercises the regional
     * {@code <region>.mtlsauth.microsoft.com} endpoint), and verifies the bound token is cached and
     * retrieved on a second call.
     */
    @Test
    void acquireTokenClientCredentials_Certificate_MtlsPop_Regional() throws Exception {
        final String clientId = KeyVaultRegistry.getMsidLabProvider()
                .getSecretByName("LabVaultAppID").getValue();

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(clientId, certificate)
                .authority(TestConstants.MICROSOFT_AUTHORITY)
                .azureRegion("westus")
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
    void acquireTokenClientCredentials_BearerAndMtlsPop_AreCacheIsolated() throws Exception {
        final String clientId = KeyVaultRegistry.getMsidLabProvider()
                .getSecretByName("LabVaultAppID").getValue();

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(clientId, certificate)
                .authority(TestConstants.MICROSOFT_AUTHORITY)
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

    /**
     * 2-leg FIC over mTLS PoP — <b>both legs</b> are mTLS-PoP requests and the final token is bound to
     * the Leg-1 certificate thumbprint (locked contract item 12).
     *
     * <p>Leg 1: the RMA/blueprint app authenticates with the SNI cert on the TLS handshake and mints a
     * federated credential (T1) with {@code fmi_path} + {@code mtlsProofOfPossession()} &rarr; T1 is
     * itself cert-bound (mints the {@code cnf}).
     *
     * <p>Leg 2: the agent app authenticates with {@code client_assertion = T1}
     * ({@code client_assertion_type = ...:jwt-pop}) <i>and</i> presents the binding certificate on the
     * TLS handshake via {@code mtlsBindingCertificate(...)} &rarr; the final token (T2) is also cert-bound.
     */
    @Test
    void acquireTokenFic_TwoLeg_MtlsPop_BothLegsBound() throws Exception {
        // LEG 1 — SNI cert (blueprint) mints a federated credential over mTLS PoP.
        ConfidentialClientApplication blueprint = ConfidentialClientApplication.builder(
                        TestConstants.AGENTIC_BLUEPRINT_CLIENT_ID, certificate)
                .authority(AGENTIC_AUTHORITY)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        IAuthenticationResult leg1 = blueprint.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(TestConstants.AGENTIC_TOKEN_EXCHANGE_SCOPE))
                        .fmiPath(TestConstants.AGENTIC_AGENT_APP_ID)
                        .mtlsProofOfPossession()
                        .build())
                .get();

        assertNotNull(leg1, "Leg 1 result should not be null");
        assertNotNull(leg1.accessToken(), "Leg 1 (T1) access token should not be null");
        assertEquals(TokenType.MTLS_POP, leg1.metadata().tokenType(),
                "Leg 1 must itself be an mTLS-PoP (cert-bound) credential");
        assertNotNull(leg1.metadata().bindingCertificate(), "Leg 1 must expose its binding certificate");
        assertEquals(expectedLabThumbprint(), leg1.metadata().bindingCertificate().thumbprintSha256(),
                "Leg 1 binding cert must be the lab SNI cert");

        String t1 = leg1.accessToken();

        // LEG 2 — agent app consumes T1 as a jwt-pop client_assertion AND presents the binding cert.
        ConfidentialClientApplication agent = ConfidentialClientApplication.builder(
                        TestConstants.AGENTIC_AGENT_APP_ID, ClientCredentialFactory.createFromClientAssertion(t1))
                .authority(AGENTIC_AUTHORITY)
                .mtlsBindingCertificate(certificate)
                .build();

        IAuthenticationResult leg2 = agent.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(TestConstants.AGENTIC_GRAPH_SCOPE))   // allow-listed resource
                        .mtlsProofOfPossession()
                        .build())
                .get();

        assertNotNull(leg2, "Leg 2 result should not be null");
        assertNotNull(leg2.accessToken(), "Leg 2 (T2) access token should not be null");
        assertFalse(leg2.accessToken().isEmpty(), "Leg 2 (T2) access token should not be empty");
        assertEquals(TokenType.MTLS_POP, leg2.metadata().tokenType(),
                "Leg 2 must also be an mTLS-PoP (cert-bound) token");
        assertNotNull(leg2.metadata().bindingCertificate(), "Leg 2 must expose its binding certificate");
        assertEquals(expectedLabThumbprint(), leg2.metadata().bindingCertificate().thumbprintSha256(),
                "Final token (T2) must be bound to the Leg-1 certificate thumbprint");
    }

    private void assertMtlsPopResult(IAuthenticationResult result, String expectedThumbprint) {
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
        assertEquals(TokenType.MTLS_POP, result.metadata().tokenType(),
                "Result token type should be MTLS_POP");

        BindingCertificate binding = result.metadata().bindingCertificate();
        assertNotNull(binding, "mTLS-PoP result must expose a binding certificate");
        assertNotNull(binding.thumbprintSha256(), "Binding certificate must expose its SHA-256 thumbprint");
        assertFalse(binding.certificateChain().isEmpty(), "Binding certificate must expose its x5c chain");
        assertEquals(expectedThumbprint, binding.thumbprintSha256(),
                "Binding certificate thumbprint must match the lab SNI cert (x5t#S256)");
    }

    private String expectedLabThumbprint() {
        return MtlsClientCertificateHelper.computeThumbprintSha256(publicCertificate);
    }
}
