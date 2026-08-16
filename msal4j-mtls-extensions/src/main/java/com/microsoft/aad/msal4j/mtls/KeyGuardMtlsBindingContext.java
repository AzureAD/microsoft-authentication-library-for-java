// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.IMtlsBindingContext;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

final class KeyGuardMtlsBindingContext implements IMtlsBindingContext {

    private final CngRsaPrivateKey privateKey;
    private final X509Certificate certificate;
    private final SSLContext sslContext;
    private final String keyId;

    KeyGuardMtlsBindingContext(
            CngRsaPrivateKey privateKey,
            X509Certificate certificate) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.keyId = calculateKeyId(certificate);
        this.sslContext = createSslContext(privateKey, certificate);
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    @Override
    public X509Certificate bindingCertificate() {
        return certificate;
    }

    @Override
    public String keyId() {
        return keyId;
    }

    void closeNativeKey() {
        privateKey.close();
    }

    private static SSLContext createSslContext(
            CngRsaPrivateKey privateKey,
            X509Certificate certificate) {
        try {
            CngProvider.installIfAbsent();
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(
                    new KeyManager[]{
                            new CngX509ExtendedKeyManager(privateKey, certificate)
                    },
                    null,
                    null);
            return context;
        } catch (Exception e) {
            privateKey.close();
            throw new MtlsMsiException("Unable to create KeyGuard JSSE SSLContext.", e);
        }
    }

    static String calculateKeyId(X509Certificate certificate) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(certificate.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new MtlsMsiException("Unable to calculate binding certificate key ID.", e);
        }
    }
}
