package com.microsoft.aad.msal4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

class DefaultHttpClient implements IHttpClient {

    private final Proxy proxy;
    private final SSLSocketFactory sslSocketFactory;

    DefaultHttpClient(Proxy proxy, SSLSocketFactory sslSocketFactory){
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
    }

    public IHttpResponse send(HttpRequest request) throws Exception{

        HttpResponse response = null;
        if (request.getHttpMethod() == HttpMethod.GET) {
            response = executeHttpGet(request);
        } else if (request.getHttpMethod() == HttpMethod.POST) {
            response = executeHttpPost(request);
        }
        return response;
    }

    private HttpResponse executeHttpGet(HttpRequest httpRequest) throws Exception {

        final HttpsURLConnection conn = openConnection(httpRequest.getUrl());
        configureAdditionalHeaders(conn, httpRequest);

        return readResponseFromConnection(conn);
    }

    private HttpResponse executeHttpPost(HttpRequest httpRequest) throws Exception {

        final HttpsURLConnection conn = openConnection(httpRequest.getUrl());
        configureAdditionalHeaders(conn, httpRequest);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(httpRequest.getPostData());
            wr.flush();

            return readResponseFromConnection(conn);
        }
        finally {
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
        }
        else {
            connection = (HttpsURLConnection) finalURL.openConnection();
        }

        if (sslSocketFactory != null) {
            connection.setSSLSocketFactory(sslSocketFactory);
        }

        return connection;
    }

    private void configureAdditionalHeaders(final HttpsURLConnection conn, final HttpRequest httpRequest) {
        if (httpRequest.getHeaders() != null) {
            for (final Map.Entry<String, List<String>> entry : httpRequest.getHeaders().entrySet()) {
                for(String headerValue: entry.getValue()){
                    conn.addRequestProperty(entry.getKey(), headerValue);
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
            httpResponse.setStatusCode(responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                is = conn.getErrorStream();
                if (is != null) {
                    httpResponse.setBody(inputStreamToString(is));
                }
                return httpResponse;
            }

            is = conn.getInputStream();
            httpResponse.setHeaders(conn.getHeaderFields());
            httpResponse.setBody(inputStreamToString(is));
            return httpResponse;
        }
        finally {
            if(is != null){
                is.close();
            }
        }
    }

    private String inputStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
