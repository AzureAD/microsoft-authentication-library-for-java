// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DefaultHttpClientManagedIdentityTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void openConnection_enforcesCertificateThumbprint(boolean matchingThumbprint, @TempDir Path directory) throws Exception {
        Path keyStorePath = directory.resolve("server.p12");
        Path javaHome = Paths.get(System.getProperty("java.home"));
        if ("jre".equalsIgnoreCase(javaHome.getFileName().toString())) {
            javaHome = javaHome.getParent();
        }
        String keytool = javaHome.resolve("bin").resolve(
                System.getProperty("os.name").startsWith("Windows") ? "keytool.exe" : "keytool").toString();
        Process generator = new ProcessBuilder(keytool, "-genkeypair", "-alias", "server",
                "-keystore", keyStorePath.toString(), "-storetype", "PKCS12",
                "-storepass", "test-password", "-keypass", "test-password",
                "-keyalg", "RSA", "-keysize", "2048", "-validity", "2",
                "-dname", "CN=localhost", "-ext", "SAN=ip:127.0.0.1", "-noprompt")
                .redirectErrorStream(true).redirectOutput(directory.resolve("keytool.log").toFile()).start();
        try {
            assertTrue(generator.waitFor(30, TimeUnit.SECONDS), "Test certificate generation timed out");
            assertEquals(0, generator.exitValue(), "Test certificate generation failed");
        } finally {
            if (generator.isAlive()) {
                generator.destroyForcibly();
            }
        }
        char[] password = "test-password".toCharArray();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (InputStream input = Files.newInputStream(keyStorePath)) {
            keyStore.load(input, password);
        }
        X509Certificate certificate = (X509Certificate) keyStore.getCertificate("server");

        KeyManagerFactory keyManagers = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagers.init(keyStore, password);
        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(keyManagers.getKeyManagers(), null, null);

        KeyStore trustStore = KeyStore.getInstance("JKS");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("server", certificate);
        TrustManagerFactory trustManagers = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagers.init(trustStore);
        SSLContext clientContext = SSLContext.getInstance("TLS");
        clientContext.init(null, trustManagers.getTrustManagers(), null);

        AtomicBoolean secretReceived = new AtomicBoolean();
        HttpsServer server = HttpsServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(serverContext));
        server.createContext("/", exchange -> {
            secretReceived.set("dummy-secret".equals(exchange.getRequestHeaders().getFirst("secret")));
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        HttpsURLConnection connection = null;
        try {
            URL endpoint = new URL("https://127.0.0.1:" + server.getAddress().getPort() + "/");
            connection = (HttpsURLConnection) endpoint.openConnection();
            connection.setSSLSocketFactory(clientContext.getSocketFactory());
            URL url = mock(URL.class);
            when(url.getProtocol()).thenReturn("https");
            when(url.openConnection()).thenReturn(connection);

            StringBuilder thumbprint = new StringBuilder();
            for (byte value : MessageDigest.getInstance("SHA-1").digest(certificate.getEncoded())) {
                thumbprint.append(String.format("%02x", value & 0xff));
            }
            IEnvironmentVariables environment = mock(IEnvironmentVariables.class);
            when(environment.getEnvironmentVariable(Constants.IDENTITY_SERVER_THUMBPRINT))
                    .thenReturn(matchingThumbprint ? thumbprint.toString() : "0000000000000000000000000000000000000000");

            try (MockedStatic<AbstractManagedIdentitySource> source = mockStatic(AbstractManagedIdentitySource.class)) {
                source.when(AbstractManagedIdentitySource::getEnvironmentVariables).thenReturn(environment);
                DefaultHttpClientManagedIdentity client = new DefaultHttpClientManagedIdentity(
                        null, clientContext.getSocketFactory(), 5000, 5000);
                HttpURLConnection configuredConnection = client.openConnection(url);
                configuredConnection.setRequestProperty("secret", "dummy-secret");

                if (matchingThumbprint) {
                    assertEquals(200, configuredConnection.getResponseCode());
                    assertTrue(secretReceived.get());
                } else {
                    assertThrows(SSLException.class, configuredConnection::getResponseCode);
                    assertFalse(secretReceived.get());
                }
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            server.stop(0);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void openConnection_rejectsMissingThumbprintBeforeOpeningConnection(String thumbprint) throws Exception {
        URL url = mock(URL.class);
        when(url.getProtocol()).thenReturn("https");
        IEnvironmentVariables environment = mock(IEnvironmentVariables.class);
        when(environment.getEnvironmentVariable(Constants.IDENTITY_SERVER_THUMBPRINT)).thenReturn(thumbprint);

        try (MockedStatic<AbstractManagedIdentitySource> source = mockStatic(AbstractManagedIdentitySource.class)) {
            source.when(AbstractManagedIdentitySource::getEnvironmentVariables).thenReturn(environment);
            DefaultHttpClientManagedIdentity client = new DefaultHttpClientManagedIdentity(null, null, 1000, 2000);

            SSLException exception = assertThrows(SSLException.class, () -> client.openConnection(url));
            assertTrue(exception.getMessage().contains("IDENTITY_SERVER_THUMBPRINT"));
            verify(url, never()).openConnection();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http", "file", "ftp"})
    void openConnection_rejectsNonHttpsBeforeOpeningConnection(String protocol) throws Exception {
        URL url = mock(URL.class);
        when(url.getProtocol()).thenReturn(protocol);
        DefaultHttpClientManagedIdentity client = new DefaultHttpClientManagedIdentity(null, null, 1000, 2000);

        SSLException exception = assertThrows(SSLException.class, () -> client.openConnection(url));
        assertTrue(exception.getMessage().contains("HTTPS"));
        verify(url, never()).openConnection();
    }
}
