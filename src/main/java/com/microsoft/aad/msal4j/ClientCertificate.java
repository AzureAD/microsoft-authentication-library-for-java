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
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;

import lombok.Getter;
import lombok.experimental.Accessors;

final class ClientCertificate implements IClientCertificate {

    private final static int MIN_KEY_SIZE_IN_BITS = 2048;

    @Accessors(fluent = true)
    @Getter
    private final PrivateKey privateKey;

    private final X509Certificate publicKeyCertificate;

    private final List<X509Certificate> publicKeyCertificateChain;

    ClientCertificate
            (PrivateKey privateKey, X509Certificate publicKeyCertificate, List<X509Certificate> publicKeyCertificateChain) {
        if (privateKey == null) {
            throw new NullPointerException("PrivateKey is null or empty");
        }

        this.privateKey = privateKey;

        if (privateKey instanceof RSAPrivateKey) {
            if (((RSAPrivateKey) privateKey).getModulus().bitLength() < MIN_KEY_SIZE_IN_BITS) {
                throw new IllegalArgumentException(
                        "certificate key size must be at least " + MIN_KEY_SIZE_IN_BITS);
            }
        } else if ("sun.security.mscapi.RSAPrivateKey".equals(privateKey.getClass().getName()) ||
                "sun.security.mscapi.CPrivateKey".equals(privateKey.getClass().getName())) {
            try {
                Method method = privateKey.getClass().getMethod("length");
                method.setAccessible(true);
                if ((int) method.invoke(privateKey) < MIN_KEY_SIZE_IN_BITS) {
                    throw new IllegalArgumentException(
                            "certificate key size must be at least " + MIN_KEY_SIZE_IN_BITS);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException("error accessing sun.security.mscapi.RSAPrivateKey length: "
                        + ex.getMessage());
            }
        } else {
            throw new IllegalArgumentException(
                    "certificate key must be an instance of java.security.interfaces.RSAPrivateKey or" +
                            " sun.security.mscapi.RSAPrivateKey");
        }

        this.publicKeyCertificate = publicKeyCertificate;
        this.publicKeyCertificateChain = publicKeyCertificateChain;
    }

    public String publicCertificateHash()
            throws CertificateEncodingException, NoSuchAlgorithmException {

        return Base64.getEncoder().encodeToString(ClientCertificate
                .getHash(this.publicKeyCertificate.getEncoded()));
    }

    public List<String> getEncodedPublicKeyCertificateOrCertificateChain() throws CertificateEncodingException {
        List<String> result = new ArrayList<>();

        if (publicKeyCertificateChain != null && publicKeyCertificateChain.size() > 0) {
            for (X509Certificate cert : publicKeyCertificateChain) {
                result.add(Base64.getEncoder().encodeToString(cert.getEncoded()));
            }
        } else {
            result.add(Base64.getEncoder().encodeToString(publicKeyCertificate.getEncoded()));
        }
        return result;
    }

    static ClientCertificate create(final InputStream pkcs12Certificate, final String password)
            throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableKeyException {

        final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
        keystore.load(pkcs12Certificate, password.toCharArray());

        final Enumeration<String> aliases = keystore.aliases();
        if (!aliases.hasMoreElements()) {
            throw new IllegalArgumentException("certificate not loaded from input stream");
        }
        String alias = aliases.nextElement();
        if (aliases.hasMoreElements()) {
            throw new IllegalArgumentException("more than one certificate alias found in input stream");
        }

        ArrayList<X509Certificate> publicKeyCertificateChain = null;
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());
        X509Certificate publicKeyCertificate = (X509Certificate) keystore.getCertificate(alias);
        Certificate[] chain = keystore.getCertificateChain(alias);

        if (chain != null) {
            publicKeyCertificateChain = new ArrayList<>();
            for (Certificate c : chain) {
                publicKeyCertificateChain.add((X509Certificate) c);
            }
        }
        return new ClientCertificate(privateKey, publicKeyCertificate, publicKeyCertificateChain);
    }

    static ClientCertificate create(final PrivateKey key, final X509Certificate publicKeyCertificate) {
        return new ClientCertificate(key, publicKeyCertificate, null);
    }

    private static byte[] getHash(final byte[] inputBytes) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(inputBytes);
        return md.digest();
    }
}
