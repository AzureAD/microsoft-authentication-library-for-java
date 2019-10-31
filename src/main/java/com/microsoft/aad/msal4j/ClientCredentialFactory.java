// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Factory for creating client credentials used in confidential client flows
 */
public class ClientCredentialFactory {

    /**
     * Static method to create a {@link ClientSecret} instance from a client secret
     * @param secret secret of application requesting a token
     * @return {@link ClientSecret}
     */
    public static IClientSecret createFromSecret(String secret){
        return new ClientSecret(secret);
    }

    /**
     * Static method to create a {@link AsymmetricKeyCredential} instance from a certificate
     * @param pkcs12Certificate InputStream containing PCKS12 formatted certificate
     * @param password certificate password
     * @return {@link AsymmetricKeyCredential}
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws IOException
     */
    public static IAsymmetricKeyCredential createFromCertificate(final InputStream pkcs12Certificate, final String password)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, IOException {
        return AsymmetricKeyCredential.create(pkcs12Certificate, password);
    }

    /**
     * Static method to create a {@link AsymmetricKeyCredential} instance.
     * @param key  RSA private key to sign the assertion.
     * @param publicCertificate x509 public certificate used for thumbprint
     * @return {@link AsymmetricKeyCredential}
     */
    public static IAsymmetricKeyCredential createFromCertificate(final PrivateKey key, final X509Certificate publicCertificate) {
        return AsymmetricKeyCredential.create(key, publicCertificate);
    }

    /**
     * Static method to create a {@link ClientAssertion} instance.
     * @param clientAssertion Jwt token encoded as a base64 URL encoded string
     * @return {@link ClientAssertion}
     */
    public static IClientAssertion createFromClientAssertion(String clientAssertion){
        return new ClientAssertion(clientAssertion);
    }
}
