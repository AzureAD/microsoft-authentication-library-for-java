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
     *
     * @param secret secret of application requesting a token
     * @return {@link ClientSecret}
     */
    public static IClientCredential createFromSecret(String secret){
        return new ClientSecret(secret);
    }

    /**
     *
     * @param pkcs12Certificate InputStream containing PCKS12 formatted certificate
     * @param password certificate password
     * @return {@link IClientCredential}
     * @throws CertificateException
     * @throws UnrecoverableKeyException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws NoSuchProviderException
     * @throws IOException
     */
    public static IClientCredential createFromCertificate(final InputStream pkcs12Certificate, final String password)
            throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException,
            KeyStoreException, NoSuchProviderException, IOException {
        return AsymmetricKeyCredential.create(pkcs12Certificate, password);
    }

    /**
     *
     * @param key  RSA private key to sign the assertion.
     * @param publicCertificate x509 public certificate used for thumbprint
     * @return {@link IClientCredential}
     */
    public static IClientCredential createFromCertificate(final PrivateKey key, final X509Certificate publicCertificate) {
        return AsymmetricKeyCredential.create(key, publicCertificate);
    }

    /**
     *
     * @param clientAssertion Jwt token encoded as a base64 URL encoded string
     * @return {@link IClientCredential}
     */
    public static IClientCredential createFromClientAssertion(String clientAssertion){
        return new ClientAssertion(clientAssertion);
    }
}
