// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedKeyManager;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyGuardMtlsBindingContextSslTest {

    private static final char[] PASSWORD = "changeit".toCharArray();

    @Test
    void softwareKeyManagerCompletesMutualTlsHandshake() throws Exception {
        KeyStore keyStore = loadKeyStore();
        KeyManagerFactory keyManagerFactory = keyManagerFactory(keyStore);
        X509ExtendedKeyManager keyManager =
                extendedKeyManager(keyManagerFactory.getKeyManagers());
        TrustManagerFactory trustManagerFactory = trustManagerFactory(keyStore);

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

    @Test
    void softwareKeyManagerCompletesMutualTlsEngineHandshake() throws Exception {
        KeyStore keyStore = loadKeyStore();
        KeyManagerFactory keyManagerFactory = keyManagerFactory(keyStore);
        X509ExtendedKeyManager keyManager =
                extendedKeyManager(keyManagerFactory.getKeyManagers());
        TrustManagerFactory trustManagerFactory = trustManagerFactory(keyStore);

        SSLContext clientContext = KeyGuardMtlsBindingContext.createSslContext(
                keyManager,
                trustManagerFactory.getTrustManagers());
        SSLContext serverContext = SSLContext.getInstance("TLSv1.2");
        serverContext.init(
                keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(),
                null);

        SSLEngine client = clientContext.createSSLEngine("localhost", 443);
        client.setUseClientMode(true);
        client.setEnabledProtocols(new String[]{"TLSv1.2"});
        SSLEngine server = serverContext.createSSLEngine();
        server.setUseClientMode(false);
        server.setNeedClientAuth(true);
        server.setEnabledProtocols(new String[]{"TLSv1.2"});

        completeHandshake(client, server);

        Certificate[] peerCertificates = server.getSession().getPeerCertificates();
        assertTrue(peerCertificates.length > 0);
        assertArrayEquals(
                keyManager.getCertificateChain(
                        keyManager.chooseEngineClientAlias(
                                new String[]{"RSA"},
                                null,
                                client))[0].getEncoded(),
                ((X509Certificate) peerCertificates[0]).getEncoded());
        assertEquals("TLSv1.2", client.getSession().getProtocol());
    }

    private static void completeHandshake(
            SSLEngine client,
            SSLEngine server) throws Exception {
        int packetBufferSize = Math.max(
                client.getSession().getPacketBufferSize(),
                server.getSession().getPacketBufferSize());
        int applicationBufferSize = Math.max(
                client.getSession().getApplicationBufferSize(),
                server.getSession().getApplicationBufferSize());
        ByteBuffer empty = ByteBuffer.allocate(0);
        ByteBuffer clientToServer = ByteBuffer.allocate(packetBufferSize * 2);
        ByteBuffer serverToClient = ByteBuffer.allocate(packetBufferSize * 2);
        ByteBuffer clientApplication = ByteBuffer.allocate(applicationBufferSize);
        ByteBuffer serverApplication = ByteBuffer.allocate(applicationBufferSize);

        client.beginHandshake();
        server.beginHandshake();

        for (int step = 0; step < 1000; step++) {
            runDelegatedTasks(client);
            runDelegatedTasks(server);

            boolean progressed = false;
            if (client.getHandshakeStatus()
                    == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                clientToServer.clear();
                checkResult(client.wrap(empty, clientToServer));
                clientToServer.flip();
                while (clientToServer.hasRemaining()) {
                    checkResult(server.unwrap(clientToServer, serverApplication));
                    runDelegatedTasks(server);
                }
                progressed = true;
            }

            if (server.getHandshakeStatus()
                    == SSLEngineResult.HandshakeStatus.NEED_WRAP) {
                serverToClient.clear();
                checkResult(server.wrap(empty, serverToClient));
                serverToClient.flip();
                while (serverToClient.hasRemaining()) {
                    checkResult(client.unwrap(serverToClient, clientApplication));
                    runDelegatedTasks(client);
                }
                progressed = true;
            }

            if (handshakeComplete(client) && handshakeComplete(server)) {
                return;
            }
            if (!progressed
                    && client.getHandshakeStatus()
                    != SSLEngineResult.HandshakeStatus.NEED_TASK
                    && server.getHandshakeStatus()
                    != SSLEngineResult.HandshakeStatus.NEED_TASK) {
                throw new SSLException(
                        "SSLEngine handshake stalled: client="
                                + client.getHandshakeStatus()
                                + ", server="
                                + server.getHandshakeStatus());
            }
        }
        throw new SSLException("SSLEngine handshake did not complete.");
    }

    private static void runDelegatedTasks(SSLEngine engine) {
        while (engine.getHandshakeStatus()
                == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable task;
            while ((task = engine.getDelegatedTask()) != null) {
                task.run();
            }
        }
    }

    private static boolean handshakeComplete(SSLEngine engine) {
        return engine.getHandshakeStatus()
                == SSLEngineResult.HandshakeStatus.FINISHED
                || engine.getHandshakeStatus()
                == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
    }

    private static void checkResult(SSLEngineResult result)
            throws SSLException {
        if (result.getStatus() == SSLEngineResult.Status.CLOSED) {
            throw new SSLException("SSLEngine closed during handshake.");
        }
        if (result.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW
                || result.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
            throw new SSLException(
                    "Unexpected SSLEngine buffer status during handshake: "
                            + result.getStatus());
        }
    }

    private KeyStore loadKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = getClass().getResourceAsStream(
                "/mtls-test-keystore.p12")) {
            assertNotNull(input);
            keyStore.load(input, PASSWORD);
        }
        return keyStore;
    }

    private static KeyManagerFactory keyManagerFactory(KeyStore keyStore)
            throws Exception {
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, PASSWORD);
        return keyManagerFactory;
    }

    private static TrustManagerFactory trustManagerFactory(KeyStore keyStore)
            throws Exception {
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(keyStore);
        return trustManagerFactory;
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
