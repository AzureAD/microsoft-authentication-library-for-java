package com.microsoft.aad.msal4j;

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
import java.nio.charset.StandardCharsets;
import java.util.Map;

class DefaultHttpClient implements IHttpClient {

    private final Proxy proxy;
    private final SSLSocketFactory sslSocketFactory;

    //By default, rely on the timeout behavior of the services requests are sent to
    private int connectTimeout = 0;
    private int readTimeout = 0;

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

        final HttpsURLConnection conn = openConnection(httpRequest.url());
        configureAdditionalHeaders(conn, httpRequest);

        return readResponseFromConnection(conn);
    }

    private HttpResponse executeHttpPost(HttpRequest httpRequest) throws Exception {

        final HttpsURLConnection conn = openConnection(httpRequest.url());
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

    private HttpsURLConnection openConnection(final URL finalURL)
            throws IOException {
        HttpsURLConnection connection;
        if (proxy != null) {
            connection = (HttpsURLConnection) finalURL.openConnection(proxy);
        } else {
            connection = (HttpsURLConnection) finalURL.openConnection();
        }

        if (sslSocketFactory != null) {
            connection.setSSLSocketFactory(sslSocketFactory);
        }

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        return connection;
    }

    private void configureAdditionalHeaders(final HttpsURLConnection conn, final HttpRequest httpRequest) {
        if (httpRequest.headers() != null) {
            for (final Map.Entry<String, String> entry : httpRequest.headers().entrySet()) {
                if (entry.getValue() != null) {
                    conn.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private HttpResponse readResponseFromConnection(final HttpsURLConnection conn) throws
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
        } catch (SocketTimeoutException readException) {conn.getURL();
            throw new MsalServiceException("Timeout while waiting for response from service. If custom timeouts were set, increasing them may resolve this issue. See https://aka.ms/msal4j-http-client for more information and solutions.", AuthenticationErrorCode.HTTP_TIMEOUT);
        } catch (ConnectException timeoutException) {
            throw new MsalServiceException("Exception while connecting to service, there may be network issues preventing MSAL Java from connecting. See https://aka.ms/msal4j-http-client for more information and solutions.", AuthenticationErrorCode.HTTP_TIMEOUT);
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
