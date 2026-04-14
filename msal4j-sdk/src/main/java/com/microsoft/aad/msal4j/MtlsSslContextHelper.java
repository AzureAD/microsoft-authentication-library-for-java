// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Builds a per-request {@link SSLSocketFactory} for mTLS client certificate authentication.
 *
 * <p>This is the Java equivalent of .NET's
 * {@code HttpClientHandler.ClientCertificates.Add(certificate)} pattern. It creates an
 * in-memory PKCS#12 {@link KeyStore}, loads the private key and certificate chain into it,
 * then initializes an {@link SSLContext} with a {@link KeyManagerFactory} backed by that
 * key store. The resulting socket factory presents the certificate during TLS handshake.</p>
 *
 * <p>The returned factory is intended to be used with a short-lived {@link DefaultHttpClient}
 * scoped to a single mTLS token request — it is not shared with the application-level HTTP
 * client.</p>
 */
class MtlsSslContextHelper {

    private static final char[] EMPTY_PASSWORD = new char[0];

    /**
     * Creates an {@link SSLSocketFactory} that presents the given certificate and private key
     * during TLS client authentication.
     *
     * @param privateKey the private key corresponding to the leaf certificate
     * @param certChain  the certificate chain; {@code certChain[0]} is the leaf certificate
     * @return an {@link SSLSocketFactory} configured for mTLS client authentication
     * @throws MsalClientException if the SSL context cannot be constructed
     */
    static SSLSocketFactory createSslSocketFactory(PrivateKey privateKey, X509Certificate[] certChain) {
        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setKeyEntry("mtls", privateKey, EMPTY_PASSWORD, certChain);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(ks, EMPTY_PASSWORD);

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(kmf.getKeyManagers(), null, null);

            return ctx.getSocketFactory();
        } catch (Exception e) {
            throw new MsalClientException(
                    "Failed to create mTLS SSL socket factory: " + e.getMessage(),
                    AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }
}
