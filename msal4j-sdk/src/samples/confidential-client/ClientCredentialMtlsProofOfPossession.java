// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

import com.microsoft.aad.msal4j.BindingCertificate;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.IClientCertificate;
import com.microsoft.aad.msal4j.TokenType;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Set;

/**
 * Demonstrates using a Subject-Name/Issuer (SN/I) certificate as the first-leg credential over
 * <b>mTLS Proof-of-Possession (PoP)</b>.
 *
 * <p>Instead of using the SN/I certificate to sign a {@code private_key_jwt} (x5c) client assertion
 * — which yields a <b>Bearer</b> token — the same certificate is presented as the <b>client TLS
 * certificate</b> in the mutual-TLS handshake to the token endpoint, so Entra ID (ESTS) returns an
 * <b>mTLS-bound PoP</b> access token ({@code token_type = mtls_pop}, bound via {@code cnf/x5t#S256}).
 * The credential is the same certificate; only the mechanism changes (assertion-signer &rarr; TLS
 * client cert).
 *
 * <p><b>Requirements / notes:</b>
 * <ul>
 *   <li>The authority must be <b>tenanted</b> ({@code /common} and {@code /organizations} are rejected
 *       on the mTLS PoP path).</li>
 *   <li>A region is <b>optional</b>: omit {@code azureRegion(...)} to use the global
 *       {@code mtlsauth.microsoft.com} endpoint (production-ready via ESTS-R regional failover), or set
 *       one to target {@code <region>.mtlsauth.microsoft.com} (recommended).</li>
 *   <li>ESTS gates mTLS PoP on the <b>final resource audience</b>: it must be an ESTS allow-listed
 *       resource (e.g. Azure Key Vault or MS Graph), not the client app.</li>
 *   <li>mTLS PoP is a confidential-client feature and is distinct from the broker-based Signed-HTTP-
 *       Request (SHR) PoP used by public clients. US Gov / China clouds are not yet supported.</li>
 * </ul>
 *
 * <p>See https://aka.ms/msal4j-pop for more details.
 */
class ClientCredentialMtlsProofOfPossession {

    private final static String CLIENT_ID = "";
    // Must be a tenanted authority — not /common or /organizations.
    private final static String AUTHORITY = "https://login.microsoftonline.com/<tenant>/";
    // Must be an ESTS allow-listed resource, e.g. Key Vault or MS Graph.
    private final static Set<String> SCOPE = Collections.singleton("https://vault.azure.net/.default");

    public static void main(String args[]) throws Exception {
        IClientCertificate sniCert = loadSniCertificate();

        directSniCertMtlsPop(sniCert);
    }

    /**
     * Scenario 1 — Direct SN/I certificate &rarr; mTLS-bound PoP token.
     *
     * <p>The SN/I certificate is presented as the client TLS certificate; the request carries
     * {@code token_type=mtls_pop} and <b>no</b> client assertion (ESTS resolves the SN/I trust from the
     * TLS-presented certificate).
     */
    private static void directSniCertMtlsPop(IClientCertificate sniCert) throws Exception {
        ConfidentialClientApplication cca =
                ConfidentialClientApplication
                        .builder(CLIENT_ID, sniCert)
                        .authority(AUTHORITY)          // tenanted authority required
                        // .azureRegion("westus")      // OPTIONAL — omit to use global mtlsauth.microsoft.com
                        .build();

        ClientCredentialParameters parameters =
                ClientCredentialParameters
                        .builder(SCOPE)
                        .mtlsProofOfPossession()       // request an mTLS-bound PoP token
                        .build();

        IAuthenticationResult result = cca.acquireToken(parameters).join();

        System.out.println("Access token: " + result.accessToken());
        System.out.println("Token type:   " + result.metadata().tokenType()); // TokenType.MTLS_POP

        // The binding certificate exposes public material only (x5c chain + SHA-256 thumbprint).
        BindingCertificate binding = result.metadata().bindingCertificate();
        System.out.println("Bound to cert x5t#S256: " + binding.thumbprintSha256());
    }

    /**
     * Load the SN/I certificate. In a real app this typically comes from a secure store (e.g. the OS
     * certificate store, a PKCS#12 file, or Azure Key Vault). The certificate object holds everything
     * MSAL needs to drive the mTLS handshake (private key + chain).
     */
    private static IClientCertificate loadSniCertificate() {
        PrivateKey privateKey = null;                 // load from your secure store
        X509Certificate publicCertificate = null;     // load from your secure store
        return ClientCredentialFactory.createFromCertificate(privateKey, publicCertificate);
    }
}
