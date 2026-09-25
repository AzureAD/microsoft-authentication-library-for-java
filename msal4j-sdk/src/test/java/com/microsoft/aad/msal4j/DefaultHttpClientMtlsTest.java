// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.Principal;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class DefaultHttpClientMtlsTest {
    private static final char[] SERVER_KEY_PASSWORD = "changeit".toCharArray();
    private static final String SERVER_KEY_STORE =
            "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";

    @Test
    void realConnectionPathInstallsRequestTlsFactoryAndDisablesRedirects() throws Exception {
        RecordingHttpsURLConnection connection = new RecordingHttpsURLConnection();
        URL url = new URL(null, "https://mtlsauth.microsoft.com/tenant/token",
                new URLStreamHandler() {
                    @Override
                    protected URLConnection openConnection(URL ignored) {
                        return connection;
                    }
                });
        SSLContext requestContext = SSLContext.getDefault();
        HttpRequest request = new HttpRequest(HttpMethod.POST, url.toString())
                .sslContext(requestContext)
                .followRedirects(false);
        javax.net.ssl.SSLSocketFactory requestFactory = request.sslSocketFactory();

        DefaultHttpClient client = new DefaultHttpClient(null, null, 123, 456);
        HttpsURLConnection opened = (HttpsURLConnection) client.openConnection(url, request);

        assertSame(connection, opened);
        assertSame(requestFactory, opened.getSSLSocketFactory());
        assertFalse(opened.getInstanceFollowRedirects());
    }

    @Test
    void tls12HandshakePresentsBindingCertificate() throws Exception {
        assertMutualTlsHandshake("TLSv1.2");
    }

    @Test
    void tls13HandshakePresentsBindingCertificateWhenSupported() throws Exception {
        assumeTrue(Arrays.asList(SSLContext.getDefault().getSupportedSSLParameters().getProtocols())
                .contains("TLSv1.3"));
        assertMutualTlsHandshake("TLSv1.3");
    }

    @Test
    void realTlsConnectionDoesNotReachRedirectTarget() throws Exception {
        MtlsBindingContext binding = bindingContext();
        SSLContext requestContext = clientSslContext(
                "TLSv1.2", binding, serverCertificate());
        try (RedirectTarget target = new RedirectTarget();
             LocalMtlsServer source = new LocalMtlsServer(
                     "TLSv1.2",
                     "HTTP/1.1 302 Found\r\nLocation: https://127.0.0.1:"
                             + target.port() + "/redirected\r\nContent-Length: 0\r\nConnection: close\r\n\r\n")) {
            HttpRequest request = new HttpRequest(HttpMethod.GET, source.url())
                    .sslContext(requestContext)
                    .followRedirects(false);

            IHttpResponse response = localTlsClient().send(request);

            assertEquals(302, response.statusCode());
            source.awaitExchange();
            Thread.sleep(100);
            assertFalse(target.wasReached());
        }
    }

    private static void assertMutualTlsHandshake(String protocol) throws Exception {
        MtlsBindingContext binding = bindingContext();
        SSLContext requestContext = clientSslContext(protocol, binding, serverCertificate());
        try (LocalMtlsServer server = new LocalMtlsServer(
                protocol,
                "HTTP/1.1 200 OK\r\nContent-Length: 2\r\nConnection: close\r\n\r\nok")) {
            HttpRequest request = new HttpRequest(HttpMethod.GET, server.url())
                    .sslContext(requestContext)
                    .followRedirects(false);

            IHttpResponse response = localTlsClient().send(request);

            assertEquals(200, response.statusCode());
            assertEquals("ok", response.body());
            server.awaitExchange();
            assertArrayEquals(
                    TestHelper.getX509Cert().getEncoded(),
                    server.peerCertificate().getEncoded());
        }
    }

    private static MtlsBindingContext bindingContext() {
        return MtlsBindingContext.create(
                ClientCertificate.create(TestHelper.getPrivateKey(), TestHelper.getX509Cert()));
    }

    private static SSLContext clientSslContext(
            String protocol,
            MtlsBindingContext binding,
            X509Certificate trustedCertificate) throws Exception {
        SSLContext context = SSLContext.getInstance(protocol);
        context.init(
                new KeyManager[]{binding.keyManager()},
                trustManagers(trustedCertificate),
                new SecureRandom());
        return context;
    }

    private static SSLContext serverSslContext(
            String protocol,
            X509Certificate trustedClientCertificate) throws Exception {
        KeyStore keyStore = serverKeyStore();
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, SERVER_KEY_PASSWORD);

        SSLContext context = SSLContext.getInstance(protocol);
        context.init(
                keyManagerFactory.getKeyManagers(),
                trustManagers(trustedClientCertificate),
                new SecureRandom());
        return context;
    }

    private static KeyStore serverKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(
                new ByteArrayInputStream(Base64.getDecoder().decode(SERVER_KEY_STORE)),
                SERVER_KEY_PASSWORD);
        return keyStore;
    }

    private static X509Certificate serverCertificate() throws Exception {
        return (X509Certificate) serverKeyStore().getCertificate("server");
    }

    private static TrustManager[] trustManagers(X509Certificate certificate) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("trusted", certificate);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        factory.init(trustStore);
        return factory.getTrustManagers();
    }

    private static DefaultHttpClient localTlsClient() {
        return new DefaultHttpClient(null, null, 5000, 5000);
    }

    private static final class LocalMtlsServer implements AutoCloseable {
        private final SSLServerSocket serverSocket;
        private final Thread thread;
        private final AtomicReference<X509Certificate> peerCertificate = new AtomicReference<>();
        private final AtomicReference<Throwable> failure = new AtomicReference<>();

        private LocalMtlsServer(
                String protocol,
                String response) throws Exception {
            SSLContext serverContext = serverSslContext(
                    protocol, TestHelper.getX509Cert());
            serverSocket = (SSLServerSocket) serverContext.getServerSocketFactory()
                    .createServerSocket(0, 1, InetAddress.getLoopbackAddress());
            serverSocket.setEnabledProtocols(new String[]{protocol});
            serverSocket.setNeedClientAuth(true);
            thread = new Thread(() -> serve(response), "msal-mtls-test-server");
            thread.start();
        }

        private String url() {
            return "https://127.0.0.1:" + serverSocket.getLocalPort() + "/";
        }

        private void serve(String response) {
            try (SSLSocket socket = (SSLSocket) serverSocket.accept()) {
                socket.startHandshake();
                peerCertificate.set((X509Certificate)
                        socket.getSession().getPeerCertificates()[0]);
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.US_ASCII));
                String header;
                while ((header = reader.readLine()) != null && !header.isEmpty()) {
                    // Consume the request headers before responding.
                }
                OutputStream output = socket.getOutputStream();
                output.write(response.getBytes(StandardCharsets.US_ASCII));
                output.flush();
            } catch (Throwable throwable) {
                if (!serverSocket.isClosed()) {
                    failure.set(throwable);
                }
            }
        }

        private X509Certificate peerCertificate() {
            return peerCertificate.get();
        }

        private void awaitExchange() throws Exception {
            thread.join(5000);
            assertFalse(thread.isAlive(), "Local TLS server did not complete the exchange.");
            if (failure.get() != null) {
                throw new AssertionError("Local TLS server failed.", failure.get());
            }
            assertTrue(peerCertificate.get() != null, "Client certificate was not presented.");
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            thread.join(5000);
        }
    }

    private static final class RedirectTarget implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final AtomicBoolean reached = new AtomicBoolean();
        private final Thread thread;

        private RedirectTarget() throws IOException {
            serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            thread = new Thread(() -> {
                try (Socket ignored = serverSocket.accept()) {
                    reached.set(true);
                } catch (IOException ignored) {
                    // Closing the listener ends a redirect target that was not reached.
                }
            }, "msal-mtls-redirect-target");
            thread.start();
        }

        private int port() {
            return serverSocket.getLocalPort();
        }

        private boolean wasReached() {
            return reached.get();
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            thread.join(5000);
        }
    }

    private static final class RecordingHttpsURLConnection extends HttpsURLConnection {
        private RecordingHttpsURLConnection() throws Exception {
            super(new URL("https://mtlsauth.microsoft.com"));
        }

        @Override public void disconnect() { }
        @Override public boolean usingProxy() { return false; }
        @Override public void connect() throws IOException { }
        @Override public String getCipherSuite() { return null; }
        @Override public Certificate[] getLocalCertificates() { return null; }
        @Override public Certificate[] getServerCertificates() { return null; }
        @Override public Principal getPeerPrincipal() { return null; }
        @Override public Principal getLocalPrincipal() { return null; }
    }
}
