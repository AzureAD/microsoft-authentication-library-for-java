// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
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
 * <p>The source certificate is resolved from the request/app credential (direct SN/I cert or FIC Leg 1),
 * or from a configured {@code mtlsBindingCertificate} (FIC Leg 2 where authentication is a federated
 * assertion). Only public material is ever surfaced; the private key stays inside the in-memory key store.
 */
final class MtlsClientCertificateHelper {

    private static final String KEY_ENTRY_ALIAS = "msal-mtls-binding-cert";

    private MtlsClientCertificateHelper() {
    }

    /**
     * Resolves the certificate to present as the client TLS certificate for an mTLS PoP request: the
     * request/app authentication credential if it is a certificate (direct SN/I cert or FIC Leg 1),
     * otherwise the application's configured {@code mtlsBindingCertificate} (FIC Leg 2, assertion-authenticated).
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

        if (application.mtlsBindingCertificate() != null) {
            return application.mtlsBindingCertificate();
        }

        throw new MsalClientException(
                "mTLS Proof-of-Possession requires a client certificate. Configure the application with a " +
                        "certificate credential, or set mtlsBindingCertificate(...) when authenticating with a " +
                        "client assertion.",
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

            char[] password = new char[0];
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(null, null);
            keyStore.setKeyEntry(KEY_ENTRY_ALIAS, certificate.privateKey(), password, chain.toArray(new Certificate[0]));

            KeyManagerFactory keyManagerFactory =
                    KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, password);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            return sslContext.getSocketFactory();
        } catch (MsalClientException e) {
            throw e;
        } catch (GeneralSecurityException | IOException e) {
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
}
