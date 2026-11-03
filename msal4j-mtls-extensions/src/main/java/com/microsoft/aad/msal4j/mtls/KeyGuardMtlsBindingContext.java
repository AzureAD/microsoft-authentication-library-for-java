// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.IMtlsBindingContext;
import com.microsoft.aad.msal4j.MtlsBindingStrength;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Enumeration;

final class KeyGuardMtlsBindingContext implements IMtlsBindingContext {

    private final CngRsaPrivateKey privateKey;
    private final X509Certificate certificate;
    private final CngX509ExtendedKeyManager keyManager;
    private final SSLContext sslContext;
    private final String keyId;
    private final RotatingTlsContext rotatingContext;

    KeyGuardMtlsBindingContext(
            CngRsaPrivateKey privateKey,
            X509Certificate certificate) {
        this(privateKey, certificate, null);
    }

    KeyGuardMtlsBindingContext(
            CngRsaPrivateKey privateKey,
            X509Certificate certificate,
            RotatingTlsContext rotatingContext) {
        this.privateKey = privateKey;
        this.certificate = certificate;
        this.rotatingContext = rotatingContext;
        this.keyId = calculateKeyId(certificate);
        this.keyManager =
                new CngX509ExtendedKeyManager(privateKey, certificate);
        try {
            this.sslContext = createSslContext(keyManager, null);
        } catch (RuntimeException e) {
            privateKey.close();
            throw e;
        }
    }

    @Override
    public SSLContext sslContext() {
        return sslContext;
    }

    @Override
    public MtlsBindingStrength bindingStrength() {
        return MtlsBindingStrength.KEY_GUARD;
    }

    @Override
    public X509ExtendedKeyManager keyManager() {
        return keyManager;
    }

    @Override
    public SSLContext rotatingSslContext() {
        return rotatingContext == null ? sslContext : rotatingContext.sslContext;
    }

    @Override
    public X509ExtendedKeyManager rotatingKeyManager() {
        return rotatingContext == null ? keyManager : rotatingContext.keyManager;
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
        if (rotatingContext != null) {
            rotatingContext.keyManager.retire(keyManager);
        }
        privateKey.close();
    }

    void activate() {
        if (rotatingContext != null) {
            rotatingContext.keyManager.activate(keyManager);
            Enumeration<byte[]> sessions =
                    rotatingContext.sslContext.getClientSessionContext().getIds();
            while (sessions.hasMoreElements()) {
                SSLSession session = rotatingContext.sslContext
                        .getClientSessionContext().getSession(sessions.nextElement());
                if (session != null) {
                    session.invalidate();
                }
            }
        }
    }

    static final class RotatingTlsContext {
        final CngX509ExtendedKeyManager keyManager = new CngX509ExtendedKeyManager();
        final SSLContext sslContext = createSslContext(keyManager, null);
    }

    static SSLContext createSslContext(
            X509ExtendedKeyManager keyManager,
            TrustManager[] trustManagers) {
        try {
            CngProvider.installIfAbsent();
            SSLContext context = SSLContext.getInstance("TLSv1.2");
            context.init(
                    new X509ExtendedKeyManager[]{keyManager},
                    trustManagers,
                    null);
            return context;
        } catch (Exception e) {
            throw new MtlsMsiException("Unable to create mTLS JSSE SSLContext.", e);
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
