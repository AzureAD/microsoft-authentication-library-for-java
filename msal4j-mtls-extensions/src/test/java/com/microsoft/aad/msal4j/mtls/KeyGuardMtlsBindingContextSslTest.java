// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.InputStream;
import java.net.InetAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class KeyGuardMtlsBindingContextSslTest {

    private static final char[] PASSWORD = "changeit".toCharArray();

    @Test
    void softwareKeyManagerCompletesMutualTlsHandshake() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = getClass().getResourceAsStream(
                "/mtls-test-keystore.p12")) {
            assertNotNull(input);
            keyStore.load(input, PASSWORD);
        }

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD);
        X509ExtendedKeyManager keyManager =
                extendedKeyManager(keyManagerFactory.getKeyManagers());

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);

        SSLContext clientContext = KeyGuardMtlsBindingContext.createSslContext(
                keyManager,
                trustManagerFactory.getTrustManagers());
        SSLContext serverContext = SSLContext.getInstance("TLSv1.2");
        serverContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                null);

        AtomicReference<Throwable> serverFailure = new AtomicReference<>();
        AtomicReference<Certificate[]> clientChain = new AtomicReference<>();

        try (SSLServerSocket server = (SSLServerSocket) serverContext
                .getServerSocketFactory()
                .createServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            server.setNeedClientAuth(true);
            server.setEnabledProtocols(new String[]{"TLSv1.2"});

            Thread serverThread = new Thread(() -> {
                try (SSLSocket socket = (SSLSocket) server.accept()) {
                    socket.setSoTimeout(5000);
                    socket.startHandshake();
                    clientChain.set(socket.getSession().getPeerCertificates());
                    socket.getInputStream().read();
                    socket.getOutputStream().write(1);
                } catch (Throwable throwable) {
                    serverFailure.set(throwable);
                }
            });
            serverThread.start();

            try (SSLSocket client = (SSLSocket) clientContext
                    .getSocketFactory()
                    .createSocket(
                            InetAddress.getLoopbackAddress(),
                            server.getLocalPort())) {
                client.setSoTimeout(5000);
                client.startHandshake();
                client.getOutputStream().write(1);
                client.getInputStream().read();
                assertEquals("TLSv1.2", client.getSession().getProtocol());
            }

            serverThread.join(5000);
            assertFalse(serverThread.isAlive());
        }

        assertNull(serverFailure.get());
        assertNotNull(clientChain.get());
    }

    private static X509ExtendedKeyManager extendedKeyManager(
            KeyManager[] keyManagers) {
        for (KeyManager keyManager : keyManagers) {
            if (keyManager instanceof X509ExtendedKeyManager) {
                return (X509ExtendedKeyManager) keyManager;
            }
        }
        throw new IllegalStateException("No X509ExtendedKeyManager available.");
    }
}
