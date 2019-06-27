// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
    public static IClientCredential create(String secret){
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
    public static IClientCredential create
            (final InputStream pkcs12Certificate, final String password)
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
    public static IClientCredential create
            (final PrivateKey key, final X509Certificate publicCertificate) {
        return AsymmetricKeyCredential.create(key, publicCertificate);
    }
}
