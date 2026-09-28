// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.security.Principal;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

class DefaultHttpClientMtlsTest {

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
