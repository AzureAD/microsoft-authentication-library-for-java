package com.microsoft.aad.msal4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class DefaultHttpClient implements IHttpClient {

    private final Proxy proxy;
    private final SSLSocketFactory sslSocketFactory;
    public int DEFAULT_CONNECT_TIMEOUT = 10000;
    public int DEFAULT_READ_TIMEOUT = 15000;

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;

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

    private HttpURLConnection openConnection(final URL finalURL)
            throws IOException {
        URLConnection connection;
        HttpURLConnection httpConnection = null;
        HttpsURLConnection httpsConnection = null;

        Boolean isHttp = false;

        if (proxy != null) {
            connection = finalURL.openConnection(proxy);
        } else {
            connection = finalURL.openConnection();
        }

        connection.setConnectTimeout(connectTimeout);
        connection.setReadTimeout(readTimeout);

        if (connection instanceof HttpURLConnection) {
            httpConnection = (HttpURLConnection) connection;
            isHttp = true;
        } else {
            httpsConnection = (HttpsURLConnection) connection;
        }

        if (!isHttp && sslSocketFactory != null) {
            httpsConnection.setSSLSocketFactory(sslSocketFactory);
        }

        return isHttp? httpConnection : httpsConnection;
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
