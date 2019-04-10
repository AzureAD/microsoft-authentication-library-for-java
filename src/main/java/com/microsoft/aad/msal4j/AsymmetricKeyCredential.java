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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Enumeration;

import org.apache.commons.codec.binary.Base64;

/**
 * Credential type containing X509 public certificate and RSA private key.
 */
public final class AsymmetricKeyCredential implements IClientCredential{
    public final static int MIN_KEY_SIZE_IN_BITS = 2048;
    private final PrivateKey key;
    private final X509Certificate publicCertificate;

    /**
     * Constructor to create credential with client id, private key and public
     * certificate.
     *
     * @param key
     *            RSA private key to sign the assertion.
     * @param publicCertificate
     *            Public certificate used for thumb print.
     */
    private AsymmetricKeyCredential(final PrivateKey key, final X509Certificate publicCertificate) {
        if (key == null) {
            throw new NullPointerException("PrivateKey is null or empty");
        }

        this.key = key;

        if (key instanceof RSAPrivateKey) {
            if(((RSAPrivateKey) key).getModulus().bitLength() < MIN_KEY_SIZE_IN_BITS) {
                throw new IllegalArgumentException(
                        "certificate key size must be at least " + MIN_KEY_SIZE_IN_BITS);
            }
        }
        else if("sun.security.mscapi.RSAPrivateKey".equals(key.getClass().getName())){
            try {
                Method method = key.getClass().getMethod("length");
                method.setAccessible(true);
                if ((int)method.invoke(key)< MIN_KEY_SIZE_IN_BITS) {
                    throw new IllegalArgumentException(
                            "certificate key size must be at least " + MIN_KEY_SIZE_IN_BITS);
                }
            } catch(NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("error accessing sun.security.mscapi.RSAPrivateKey length: "
                        + ex.getMessage());
            }
        }
        else{
            throw new IllegalArgumentException(
                    "certificate key must be an instance of java.security.interfaces.RSAPrivateKey or" +
                            " sun.security.mscapi.RSAPrivateKey");
        }

        this.publicCertificate = publicCertificate;
    }

    /**
     * Base64 encoded hash of the the public certificate.
     *
     * @return base64 encoded string
     * @throws CertificateEncodingException if an encoding error occurs
     * @throws NoSuchAlgorithmException if requested algorithm is not available in the environment
     */
    public String getPublicCertificateHash()
            throws CertificateEncodingException, NoSuchAlgorithmException {
        return Base64.encodeBase64String(AsymmetricKeyCredential
                .getHash(this.publicCertificate.getEncoded()));
    }

    /**
     * Base64 encoded public certificate.
     * 
     * @return base64 encoded string
     * @throws CertificateEncodingException if an encoding error occurs
     */
    public String getPublicCertificate() throws CertificateEncodingException {
        return Base64.encodeBase64String(this.publicCertificate.getEncoded());
    }

    /**
     * Returns private key of the credential.
     * 
     * @return private key.
     */
    public PrivateKey getKey() {
        return key;
    }

    /**
     * Static method to create KeyCredential instance.
     *
     * @param pkcs12Certificate
     *            PKCS12 certificate stream containing public and private key.
     *            Caller is responsible for handling the input stream.
     * @param password
     *            certificate password
     * @return KeyCredential instance
     * @throws KeyStoreException {@link KeyStoreException}
     * @throws NoSuchProviderException {@link NoSuchProviderException}
     * @throws NoSuchAlgorithmException {@link NoSuchAlgorithmException}
     * @throws CertificateException {@link CertificateException}
     * @throws IOException {@link IOException}
     * @throws UnrecoverableKeyException {@link UnrecoverableKeyException}
     */
    public static AsymmetricKeyCredential create(final InputStream pkcs12Certificate, final String password)
            throws KeyStoreException, NoSuchProviderException,
            NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
        keystore.load(pkcs12Certificate, password.toCharArray());
        final Enumeration<String> aliases = keystore.aliases();
        final String alias = aliases.nextElement();
        final PrivateKey key = (PrivateKey) keystore.getKey(alias,
                password.toCharArray());
        final X509Certificate publicCertificate = (X509Certificate) keystore
                .getCertificate(alias);
        return create(key, publicCertificate);
    }

    /**
     * Static method to create KeyCredential instance.
     *
     * @param key
     *            RSA private key to sign the assertion.
     * @param publicCertificate
     *            Public certificate used for thumb print.
     * @return KeyCredential instance
     */
    public static AsymmetricKeyCredential create(final PrivateKey key, final X509Certificate publicCertificate) {
        return new AsymmetricKeyCredential(key, publicCertificate);
    }

    private static byte[] getHash(final byte[] inputBytes) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(inputBytes);
        return md.digest();
    }

}
