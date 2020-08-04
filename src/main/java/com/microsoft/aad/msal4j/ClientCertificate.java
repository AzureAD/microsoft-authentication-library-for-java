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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import lombok.Getter;
import lombok.experimental.Accessors;
import java.util.Base64;
import java.util.stream.Collectors;

final class ClientCertificate implements IClientCertificate {

    private final static int MIN_KEY_SIZE_IN_BITS = 2048;

    @Accessors(fluent = true)
    @Getter
    private final PrivateKey key;

    private final List<X509Certificate> publicCertificates;

    private ClientCertificate(final PrivateKey key, final List<X509Certificate> publicCertificates) {
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
        else if("sun.security.mscapi.RSAPrivateKey".equals(key.getClass().getName()) ||
                "sun.security.mscapi.CPrivateKey".equals(key.getClass().getName())){
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

        this.publicCertificates = publicCertificates;
    }

    public String publicCertificateHash()
            throws CertificateEncodingException, NoSuchAlgorithmException {

        return Base64.getEncoder().encodeToString(ClientCertificate
                .getHash(this.publicCertificates.get(0).getEncoded()));
    }

    public List<String> publicCertificates() throws CertificateEncodingException {
        return this.publicCertificates.stream().map(cert -> {
            try {
                return Base64.getEncoder().encodeToString(cert.getEncoded());
            } catch (CertificateEncodingException e) {
                e.printStackTrace();
            }
            return null;
        }).collect(Collectors.toList());
    }

    static ClientCertificate create(final InputStream pkcs12Certificate, final String password)
            throws KeyStoreException, NoSuchProviderException,
            NoSuchAlgorithmException, CertificateException, IOException, UnrecoverableKeyException {
        final KeyStore keystore = KeyStore.getInstance("PKCS12", "SunJSSE");
        keystore.load(pkcs12Certificate, password.toCharArray());
        final Enumeration<String> aliases = keystore.aliases();
        final ArrayList<X509Certificate> publicCertificates = new ArrayList<X509Certificate>();
        PrivateKey key = null;
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keystore.isKeyEntry(alias)) {
                key = (PrivateKey) keystore.getKey(alias, password.toCharArray());
                //dig down the certificate chain and put the public keys in the keystore
                Certificate[] chain = keystore.getCertificateChain(alias);
                for (Certificate c: chain) {
                    publicCertificates.add((X509Certificate)c);
                }
            }
        }
        return new ClientCertificate(key, publicCertificates);
    }

    static ClientCertificate create(final PrivateKey key, final X509Certificate publicCertificate) {
        List<X509Certificate> certs = new ArrayList<X509Certificate>();
        certs.add(publicCertificate);
        return new ClientCertificate(key, certs);
    }

    private static byte[] getHash(final byte[] inputBytes) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1");
        md.update(inputBytes);
        return md.digest();
    }
}
