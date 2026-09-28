// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;

final class MtlsBindingContext implements IMtlsBindingContext {
    private static final String ALIAS = "msal-mtls-binding";

    private final List<X509Certificate> certificateChain;
    private final String keyId;
    private final X509ExtendedKeyManager keyManager;
    private final SSLContext sslContext;

    private MtlsBindingContext(List<X509Certificate> certificateChain,
                               String keyId,
                               X509ExtendedKeyManager keyManager,
                               SSLContext sslContext) {
        this.certificateChain = Collections.unmodifiableList(new ArrayList<>(certificateChain));
        this.keyId = keyId;
        this.keyManager = keyManager;
        this.sslContext = sslContext;
    }

    static MtlsBindingContext create(IClientCertificate certificate) {
        try {
            List<X509Certificate> chain = new ArrayList<>();
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            for (String encoded : certificate.getEncodedPublicKeyCertificateChain()) {
                byte[] der = Base64.getDecoder().decode(encoded);
                chain.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(der)));
            }
            if (chain.isEmpty() || certificate.privateKey() == null) {
                throw new MsalClientException("mTLS Proof-of-Possession requires a certificate chain and private key.",
                        AuthenticationErrorCode.MTLS_POP_ERROR);
            }
            chain.get(0).checkValidity();
            verifyPrivateKeyMatchesCertificate(certificate.privateKey(), chain.get(0));

            X509ExtendedKeyManager keyManager = new SingleCertificateKeyManager(
                    certificate.privateKey(), chain.toArray(new X509Certificate[0]));
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[]{keyManager}, null, null);
            String keyId = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(chain.get(0).getEncoded()));
            return new MtlsBindingContext(chain, keyId, keyManager, sslContext);
        } catch (MsalClientException e) {
            throw e;
        } catch (Exception e) {
            throw new MsalClientException("Failed to resolve mTLS binding certificate: " + e.getMessage(),
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    X509ExtendedKeyManager keyManager() {
        return keyManager;
    }

    @Override
    public X509Certificate bindingCertificate() {
        return certificateChain.get(0);
    }

    @Override
    public List<X509Certificate> certificateChain() {
        return certificateChain;
    }

    @Override
    public String keyId() {
        return keyId;
    }

    @Override
    public Date notBefore() {
        return new Date(bindingCertificate().getNotBefore().getTime());
    }

    @Override
    public Date notAfter() {
        return new Date(bindingCertificate().getNotAfter().getTime());
    }

    BindingCertificate diagnostics() {
        return new BindingCertificate(certificateChain, keyId);
    }

    private static void verifyPrivateKeyMatchesCertificate(
            PrivateKey privateKey,
            X509Certificate leafCertificate) throws Exception {
        if (!"RSA".equalsIgnoreCase(privateKey.getAlgorithm())
                || !"RSA".equalsIgnoreCase(leafCertificate.getPublicKey().getAlgorithm())) {
            throw new MsalClientException(
                    "mTLS Proof-of-Possession requires an RSA certificate and private key.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }

        byte[] challenge = new byte[32];
        new SecureRandom().nextBytes(challenge);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(challenge);
        byte[] signature = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(leafCertificate.getPublicKey());
        verifier.update(challenge);
        if (!verifier.verify(signature)) {
            throw new MsalClientException(
                    "The mTLS private key does not match the leaf certificate.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    private static final class SingleCertificateKeyManager extends X509ExtendedKeyManager {
        private final PrivateKey privateKey;
        private final X509Certificate[] chain;

        private SingleCertificateKeyManager(PrivateKey privateKey, X509Certificate[] chain) {
            this.privateKey = privateKey;
            this.chain = chain.clone();
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return supports(keyType) && acceptsIssuers(issuers) ? new String[]{ALIAS} : null;
        }

        @Override
        public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
            if (keyTypes != null) {
                for (String keyType : keyTypes) {
                    if (supports(keyType) && acceptsIssuers(issuers)) {
                        return ALIAS;
                    }
                }
            }
            return null;
        }

        @Override
        public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
            return chooseClientAlias(keyTypes, issuers, null);
        }

        private boolean supports(String keyType) {
            return keyType != null && keyType.equalsIgnoreCase(privateKey.getAlgorithm());
        }

        private boolean acceptsIssuers(Principal[] acceptableIssuers) {
            if (acceptableIssuers == null || acceptableIssuers.length == 0) {
                return true;
            }
            for (Principal acceptableIssuer : acceptableIssuers) {
                for (X509Certificate certificate : chain) {
                    if (acceptableIssuer.equals(certificate.getIssuerX500Principal())
                            || acceptableIssuer.equals(certificate.getSubjectX500Principal())) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override public String[] getServerAliases(String keyType, Principal[] issuers) { return null; }
        @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) { return null; }
        @Override public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) { return null; }
        @Override public X509Certificate[] getCertificateChain(String alias) { return ALIAS.equals(alias) ? chain.clone() : null; }
        @Override public PrivateKey getPrivateKey(String alias) { return ALIAS.equals(alias) ? privateKey : null; }
    }
}
