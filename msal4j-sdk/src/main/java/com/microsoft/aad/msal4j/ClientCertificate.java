// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

final class ClientCertificate implements IClientCertificate {

    /**
     * TODO: Add field description
     */
    public static final String DEFAULT_PKCS12_PASSWORD = "";

    private final PrivateKey privateKey;

    private final List<X509Certificate> publicKeyCertificateChain;

    ClientCertificate
            (PrivateKey privateKey, List<X509Certificate> publicKeyCertificateChain) {
        if (privateKey == null) {
            throw new NullPointerException("PrivateKey is null or empty");
        }

        this.privateKey = privateKey;

        this.publicKeyCertificateChain = publicKeyCertificateChain;
    }

    @Override
    /**
     * TODO: Add description
     */
    public String publicCertificateHash256()
            throws CertificateEncodingException, NoSuchAlgorithmException {

        return Base64.getEncoder().encodeToString(ClientCertificate
                .getHashSha256(publicKeyCertificateChain.get(0).getEncoded()));
    }

    /**
     * TODO: Add description
     */
    public String publicCertificateHash()
            throws CertificateEncodingException, NoSuchAlgorithmException {

        return Base64.getEncoder().encodeToString(ClientCertificate
                .getHashSha1(publicKeyCertificateChain.get(0).getEncoded()));
    }

    /**
     * Gets the encoded public key certificate chain.
     * 
     * @return the encoded public key certificate chain
     */
    public List<String> getEncodedPublicKeyCertificateChain() throws CertificateEncodingException {
        List<String> result = new ArrayList<>();

        for (X509Certificate cert : publicKeyCertificateChain) {
            result.add(Base64.getEncoder().encodeToString(cert.getEncoded()));
        }
        return result;
    }

    static ClientCertificate create(InputStream pkcs12Certificate, String password)
            throws KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException,
            CertificateException, IOException, UnrecoverableKeyException {
        // treat null password as default one - empty string
        if (password == null) {
            password = DEFAULT_PKCS12_PASSWORD;
        }

        final KeyStore keystore = KeyStore.getInstance("PKCS12");
        keystore.load(pkcs12Certificate, password.toCharArray());

        String alias = getPrivateKeyAlias(keystore);

        ArrayList<X509Certificate> publicKeyCertificateChain = new ArrayList<>();
        PrivateKey privateKey = (PrivateKey) keystore.getKey(alias, password.toCharArray());

        X509Certificate publicKeyCertificate = (X509Certificate) keystore.getCertificate(alias);
        Certificate[] chain = keystore.getCertificateChain(alias);

        if (chain != null && chain.length > 0) {
            for (Certificate c : chain) {
                publicKeyCertificateChain.add((X509Certificate) c);
            }
        } else {
            publicKeyCertificateChain.add(publicKeyCertificate);
        }

        return new ClientCertificate(privateKey, publicKeyCertificateChain);
    }

    static String getPrivateKeyAlias(KeyStore keystore) throws KeyStoreException {
        String alias = null;
        final Enumeration<String> aliases = keystore.aliases();
        while (aliases.hasMoreElements()) {
            String currentAlias = aliases.nextElement();
            if (keystore.entryInstanceOf(currentAlias, KeyStore.PrivateKeyEntry.class)) {
                if (alias != null) {
                    throw new IllegalArgumentException("more than one certificate alias found in input stream");
                }
                alias = currentAlias;
            }
        }

        if (alias == null) {
            throw new IllegalArgumentException("certificate not loaded from input stream");
        }

        return alias;
    }

    static ClientCertificate create(final PrivateKey key, final X509Certificate publicKeyCertificate) {
        return new ClientCertificate(key, Arrays.asList(publicKeyCertificate));
    }

    private static byte[] getHashSha1(final byte[] inputBytes) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-1"); // CodeQL [SM05136] ADFS scenarios require SHA-1 hashing, and we cannot remove our use until ADFS does.
        md.update(inputBytes);
        return md.digest();
    }

    private static byte[] getHashSha256(final byte[] inputBytes) throws NoSuchAlgorithmException {
        final MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(inputBytes);
        return md.digest();
    }

    /**
     * TODO: Add description
     */
    public PrivateKey privateKey() {
        return this.privateKey;
    }
}
