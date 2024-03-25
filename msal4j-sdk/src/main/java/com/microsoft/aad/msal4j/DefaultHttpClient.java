// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class DefaultHttpClient implements IHttpClient {
    private static final  Logger LOG = LoggerFactory.getLogger(DefaultHttpClient.class);

    final Proxy proxy;
    final SSLSocketFactory sslSocketFactory;

    //By default, rely on the timeout behavior of the services requests are sent to
    int connectTimeout = 0;
    int readTimeout = 0;

    DefaultHttpClient(Proxy proxy, SSLSocketFactory sslSocketFactory, Integer connectTimeout, Integer readTimeout) {
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
        if (connectTimeout != null) this.connectTimeout = connectTimeout;
        if (readTimeout != null) this.readTimeout = readTimeout;
    }

    public IHttpResponse send(HttpRequest httpRequest) throws Exception {

        HttpResponse response = null;
        if (httpRequest.httpMethod() == HttpMethod.GET) {
            response = executeHttpGet(httpRequest);
        } else if (httpRequest.httpMethod() == HttpMethod.POST) {
            response = executeHttpPost(httpRequest);
        }
        return response;
    }

    private HttpResponse executeHttpGet(HttpRequest httpRequest) throws Exception {

        final HttpURLConnection conn = openConnection(httpRequest.url());
        configureAdditionalHeaders(conn, httpRequest);

        return readResponseFromConnection(conn);
    }

    private HttpResponse executeHttpPost(HttpRequest httpRequest) throws Exception {

        final HttpURLConnection conn = openConnection(httpRequest.url());
        configureAdditionalHeaders(conn, httpRequest);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(httpRequest.body());
            wr.flush();

            return readResponseFromConnection(conn);
        } finally {
            if (wr != null) {
                wr.close();
            }
        }
    }

    HttpURLConnection openConnection(final URL finalURL)
            throws IOException {
        URLConnection connection;

        if (proxy != null) {
            connection = finalURL.openConnection(proxy);
        } else {
            connection = finalURL.openConnection();
        }

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        if (connection instanceof HttpURLConnection) {
            return (HttpURLConnection) connection;
        } else {
            HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;

            if (sslSocketFactory != null) {
                httpsConnection.setSSLSocketFactory(sslSocketFactory);
            }

            return httpsConnection;
        }
    }

    private void configureAdditionalHeaders(final HttpURLConnection conn, final HttpRequest httpRequest) {
        if (httpRequest.headers() != null) {
            for (final Map.Entry<String, String> entry : httpRequest.headers().entrySet()) {
                if (entry.getValue() != null) {
                    conn.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private HttpResponse readResponseFromConnection(final HttpURLConnection conn) throws
            IOException {
        InputStream is = null;
        try {
            HttpResponse httpResponse = new HttpResponse();
            int responseCode = conn.getResponseCode();
            httpResponse.statusCode(responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                is = conn.getErrorStream();
                if (is != null) {
                    httpResponse.addHeaders(conn.getHeaderFields());
                    httpResponse.body(inputStreamToString(is));
                }
                return httpResponse;
            }

            is = conn.getInputStream();
            httpResponse.addHeaders(conn.getHeaderFields());
            httpResponse.body(inputStreamToString(is));
            return httpResponse;
        } catch (SocketTimeoutException readException) {
            LOG.error("Timeout while waiting for response from service. If custom timeouts were set, increasing them may resolve this issue. See https://aka.ms/msal4j-http-client for more information and solutions.");

            throw readException;
        } catch (ConnectException timeoutException) {
            LOG.error("Exception while connecting to service, there may be network issues preventing MSAL Java from connecting. See https://aka.ms/msal4j-http-client for more information and solutions.");

            throw timeoutException;
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    private String inputStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
