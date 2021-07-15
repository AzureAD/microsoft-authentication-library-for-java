// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Factory for creating client credentials used in confidential client flows. For more details, see
 * https://aka.ms/msal4j-client-credentials
 */
public class ClientCredentialFactory {

    /**
     * Static method to create a {@link ClientSecret} instance from a client secret
     *
     * @param secret secret of application requesting a token
     * @return {@link ClientSecret}
     */
    public static IClientSecret createFromSecret(String secret) {
        return new ClientSecret(secret);
    }

    /**
     * Static method to create a {@link ClientCertificate} instance from a certificate
     *
     * @param pkcs12Certificate InputStream containing PCKS12 formatted certificate
     * @param password          certificate password
     * @return {@link ClientCertificate}
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws IOException
     */
    public static IClientCertificate createFromCertificate(final InputStream pkcs12Certificate, final String password)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, IOException {
        return ClientCertificate.create(pkcs12Certificate, password);
    }

    /**
     * Static method to create a {@link ClientCertificate} instance.
     *
     * @param key                  RSA private key to sign the assertion.
     * @param publicKeyCertificate x509 public certificate used for thumbprint
     * @return {@link ClientCertificate}
     */
    public static IClientCertificate createFromCertificate(final PrivateKey key, final X509Certificate publicKeyCertificate) {
        validateNotNull("publicKeyCertificate", publicKeyCertificate);

        return ClientCertificate.create(key, publicKeyCertificate);
    }

    /**
     * Static method to create a {@link ClientCertificate} instance.
     *
     * @param key                       RSA private key to sign the assertion.
     * @param publicKeyCertificateChain ordered with the user's certificate first followed by zero or more certificate authorities
     * @return {@link ClientCertificate}
     */
    public static IClientCertificate createFromCertificateChain(PrivateKey key, List<X509Certificate> publicKeyCertificateChain) {
        if (key == null || publicKeyCertificateChain == null || publicKeyCertificateChain.size() == 0) {
            throw new IllegalArgumentException("null or empty input parameter");
        }
        return new ClientCertificate(key, publicKeyCertificateChain);
    }

    /**
     * Static method to create a {@link ClientAssertion} instance.
     *
     * @param clientAssertion Jwt token encoded as a base64 URL encoded string
     * @return {@link ClientAssertion}
     */
    public static IClientAssertion createFromClientAssertion(String clientAssertion) {
        return new ClientAssertion(clientAssertion);
    }
}
