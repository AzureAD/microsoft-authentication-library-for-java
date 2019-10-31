// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

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

import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.codec.binary.Base64;

final class AsymmetricKeyCredential implements IAsymmetricKeyCredential  {

    private final static int MIN_KEY_SIZE_IN_BITS = 2048;

    @Accessors(fluent = true)
    @Getter
    private final PrivateKey key;

    private final X509Certificate publicCertificate;

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

    public String publicCertificateHash()
            throws CertificateEncodingException, NoSuchAlgorithmException {
        return Base64.encodeBase64String(AsymmetricKeyCredential
                .getHash(this.publicCertificate.getEncoded()));
    }

    public String publicCertificate() throws CertificateEncodingException {
        return Base64.encodeBase64String(this.publicCertificate.getEncoded());
    }

    static AsymmetricKeyCredential create(final InputStream pkcs12Certificate, final String password)
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

    static AsymmetricKeyCredential create(final PrivateKey key, final X509Certificate publicCertificate) {
        return new AsymmetricKeyCredential(key, publicCertificate);
    }

    private static byte[] getHash(final byte[] inputBytes) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(inputBytes);
        return md.digest();
    }
}
