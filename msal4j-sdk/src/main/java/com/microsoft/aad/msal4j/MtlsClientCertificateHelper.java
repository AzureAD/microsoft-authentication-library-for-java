// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Builds the TLS material needed to present an {@link IClientCertificate} as the client certificate in a
 * mutual-TLS (mTLS) handshake to the token endpoint, and to describe that certificate as a public
 * {@link BindingCertificate} on the result.
 *
 * <p>The source certificate is resolved from the request/app credential (direct SN/I cert or FIC Leg 1).
 * Only public material is ever surfaced; the private key is used in place (through its own provider)
 * and is never exported or copied into a key store.
 */
final class MtlsClientCertificateHelper {

    private static final String KEY_ENTRY_ALIAS = "msal-mtls-binding-cert";

    private MtlsClientCertificateHelper() {
    }

    /**
     * Resolves the certificate to present as the client TLS certificate for an mTLS PoP request: the
     * request/app authentication credential when it is a certificate (direct SN/I cert or FIC Leg 1).
     *
     * @throws MsalClientException if no certificate can be resolved
     */
    static IClientCertificate resolveBindingCertificate(ConfidentialClientApplication application,
                                                        ClientCredentialParameters parameters) {
        IClientCredential credential = application.clientCredential;
        if (parameters != null && parameters.clientCredential() != null) {
            credential = parameters.clientCredential();
        }

        if (credential instanceof IClientCertificate) {
            return (IClientCertificate) credential;
        }

        throw new MsalClientException(
                "mTLS Proof-of-Possession requires a client certificate. Configure the application with a " +
                        "certificate credential.",
                AuthenticationErrorCode.MTLS_POP_ERROR);
    }

    /**
     * Builds an {@link SSLSocketFactory} that presents the given certificate as the client certificate
     * during the TLS handshake.
     */
    static SSLSocketFactory createMtlsSocketFactory(IClientCertificate certificate) {
        if (certificate == null) {
            throw new MsalClientException(
                    "mTLS Proof-of-Possession requires a client certificate, but none was resolved.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }

        try {
            List<X509Certificate> chain = decodeCertificateChain(certificate);
            if (chain.isEmpty()) {
                throw new MsalClientException(
                        "mTLS Proof-of-Possession requires a certificate with a public key chain.",
                        AuthenticationErrorCode.MTLS_POP_ERROR);
            }

            PrivateKey privateKey = certificate.privateKey();
            if (privateKey == null) {
                throw new MsalClientException(
                        "mTLS Proof-of-Possession requires a certificate with an accessible private key.",
                        AuthenticationErrorCode.MTLS_POP_ERROR);
            }

            // Present the certificate to the TLS layer through a KeyManager that holds the live private
            // key object directly, instead of importing it into a PKCS12 KeyStore. The key may be a
            // non-exportable handle (e.g. a Windows-MY / SunMSCAPI or HSM key) whose getEncoded()
            // returns null and which therefore cannot be added to a KeyStore; the TLS handshake can
            // still sign through the key's own provider. Ordinary exportable keys use the same path.
            KeyManager[] keyManagers = {
                    new SingleCertificateKeyManager(KEY_ENTRY_ALIAS, privateKey,
                            chain.toArray(new X509Certificate[0]))
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagers, null, null);

            return sslContext.getSocketFactory();
        } catch (MsalClientException e) {
            throw e;
        } catch (GeneralSecurityException e) {
            throw new MsalClientException(
                    "Failed to build the mTLS client certificate socket factory: " + e.getMessage(),
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    /**
     * Builds a public {@link BindingCertificate} (x5c chain + SHA-256 thumbprint) for the result metadata.
     * Never includes the private key.
     */
    static BindingCertificate buildBindingCertificate(IClientCertificate certificate) {
        if (certificate == null) {
            return null;
        }

        try {
            List<X509Certificate> chain = decodeCertificateChain(certificate);
            if (chain.isEmpty()) {
                return null;
            }
            String thumbprint = computeThumbprintSha256(chain.get(0));
            return new BindingCertificate(chain, thumbprint);
        } catch (CertificateException e) {
            throw new MsalClientException(
                    "Failed to read the mTLS binding certificate: " + e.getMessage(),
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    /**
     * @return the base64url (no padding) SHA-256 thumbprint (x5t#S256) of the given certificate's KeyId,
     * used both as the public thumbprint and as a cache-isolation dimension.
     */
    static String computeThumbprintSha256(X509Certificate certificate) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(certificate.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new MsalClientException(
                    "Failed to compute the mTLS binding certificate thumbprint: " + e.getMessage(),
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    /**
     * Computes the KeyId ({@code x5t#S256}) of the leaf certificate of the given credential, used as a
     * cache-isolation dimension so tokens bound to different certificates never alias.
     */
    static String computeCertificateKeyId(IClientCertificate certificate) {
        try {
            List<X509Certificate> chain = decodeCertificateChain(certificate);
            if (chain.isEmpty()) {
                return null;
            }
            return computeThumbprintSha256(chain.get(0));
        } catch (CertificateException e) {
            throw new MsalClientException(
                    "Failed to compute the mTLS binding certificate KeyId: " + e.getMessage(),
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    private static List<X509Certificate> decodeCertificateChain(IClientCertificate certificate)
            throws CertificateException {
        List<X509Certificate> chain = new ArrayList<>();
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
        for (String encoded : certificate.getEncodedPublicKeyCertificateChain()) {
            byte[] der = Base64.getDecoder().decode(encoded);
            chain.add((X509Certificate) certificateFactory.generateCertificate(new ByteArrayInputStream(der)));
        }
        return chain;
    }

    /**
     * An {@link X509ExtendedKeyManager} that always presents a single, pre-resolved client certificate
     * and its private key during a TLS handshake. It holds the live {@link PrivateKey} reference rather
     * than importing the key into a {@link java.security.KeyStore}, so it works with non-exportable keys
     * (e.g. Windows-MY / SunMSCAPI or HSM-backed keys) whose {@code getEncoded()} returns {@code null}.
     * Signing is delegated to the key's own provider by the SSL engine.
     */
    private static final class SingleCertificateKeyManager extends X509ExtendedKeyManager {

        private final String alias;
        private final PrivateKey privateKey;
        private final X509Certificate[] chain;

        SingleCertificateKeyManager(String alias, PrivateKey privateKey, X509Certificate[] chain) {
            this.alias = alias;
            this.privateKey = privateKey;
            this.chain = chain;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[]{alias};
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            return alias;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyType, Principal[] issuers, SSLEngine engine) {
            return alias;
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return null;
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            return null;
        }

        @Override
        public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
            return null;
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return chain.clone();
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return privateKey;
        }
    }
}
