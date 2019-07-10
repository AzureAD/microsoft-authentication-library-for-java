// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

class HttpHelper {

    static String executeHttpRequest(Logger log,
                                     HttpMethod httpMethod,
                                     final String url,
                                     final Map<String, String> headers,
                                     String postData,
                                     RequestContext requestContext,
                                     final ServiceBundle serviceBundle) throws Exception{

        HttpEvent httpEvent = new HttpEvent();
        String response = null;

        try(TelemetryHelper telemetryHelper = serviceBundle.getTelemetryManager().createTelemetryHelper(
                requestContext.getTelemetryRequestId(),
                requestContext.getClientId(),
                httpEvent,
                false)){

            URL endpointUrl = new URL(url);
            httpEvent.setHttpPath(endpointUrl.toURI());
            if(!StringHelper.isBlank(endpointUrl.getQuery())){
                httpEvent.setQueryParameters(endpointUrl.getQuery());
            }

            if(httpMethod == HttpMethod.GET) {
                httpEvent.setHttpMethod("GET");
                response = executeHttpGet(log, endpointUrl, headers, serviceBundle, httpEvent);
            } else if (httpMethod == HttpMethod.POST){
                httpEvent.setHttpMethod("POST");
                response = executeHttpPost(log, endpointUrl, postData, headers, serviceBundle, httpEvent);
            }
        }
        return response;
    }

    private static String executeHttpGet(final Logger log, final URL url,
                                         final Map<String, String> headers,
                                         final ServiceBundle serviceBundle,
                                         HttpEvent httpEvent) throws Exception {
        final HttpsURLConnection conn = HttpHelper.openConnection(url, serviceBundle);
        configureAdditionalHeaders(conn, headers);

        return getResponse(log, headers, conn, httpEvent);
    }

    private static String executeHttpPost(final Logger log, final URL url,
                                          String postData, final Map<String, String> headers,
                                          final ServiceBundle serviceBundle, HttpEvent httpEvent)
            throws Exception {
        final HttpsURLConnection conn = HttpHelper.openConnection(url, serviceBundle);
        configureAdditionalHeaders(conn, headers);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);

        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();

            return getResponse(log, headers, conn, httpEvent);
        }
        finally {
            if (wr != null) {
                wr.close();
            }
        }
    }

    private static String getResponse(Logger log,
                                      Map<String, String> headers,
                                      HttpsURLConnection conn,
                                      HttpEvent httpEvent) throws IOException {
        String response = readResponseFromConnection(conn, httpEvent);
        if (headers != null) {
            HttpHelper.verifyReturnedCorrelationId(log, conn, headers
                    .get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
        }

        if(!StringHelper.isBlank(conn.getHeaderField("User-Agent"))){
            httpEvent.setUserAgent(conn.getHeaderField("User-Agent"));
        }
        setXmsClientTelemetryInfo(conn, httpEvent);

        return response;
    }

    static HttpsURLConnection openConnection(final URL finalURL, final ServiceBundle serviceBundle)
            throws IOException {
        HttpsURLConnection connection;
        if (serviceBundle.getProxy() != null) {
            connection = (HttpsURLConnection) finalURL.openConnection(serviceBundle.getProxy());
        }
        else {
            connection = (HttpsURLConnection) finalURL.openConnection();
        }

        if (serviceBundle.getSslSocketFactory() != null) {
            connection.setSSLSocketFactory(serviceBundle.getSslSocketFactory());
        }

        return connection;
    }

    static void configureAdditionalHeaders(
            final HttpsURLConnection conn, final Map<String, String> headers) {
        if (headers != null) {
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
    }

    static void verifyReturnedCorrelationId(Logger log,
                                            HttpsURLConnection conn, String sentCorrelationId) {
        if (StringHelper
                .isBlank(conn
                        .getHeaderField(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME))
                || !conn.getHeaderField(
                ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME)
                .equals(sentCorrelationId)) {

            String msg = LogHelper.createMessage(
                    String.format(
                            "Sent (%s) Correlation Id is not same as received (%s).",
                            sentCorrelationId,
                            conn.getHeaderField(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME)),
                    sentCorrelationId);
            log.info(msg);
        }
    }

    static String readResponseFromConnection(final HttpsURLConnection conn, HttpEvent httpEvent)
            throws AuthenticationException, IOException {
        InputStream is = null;
        try {
            int responseCode = conn.getResponseCode();
            httpEvent.setHttpResponseStatus(responseCode);
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String msg = "Server returned HTTP response code: " +
                        responseCode + " for URL : " + conn.getURL();
                is = conn.getErrorStream();
                if (is != null) {
                    msg = msg + ", Error details : " + inputStreamToString(is);
                }
                httpEvent.setOauthErrorCode(AuthenticationErrorCode.UNKNOWN.toString());
                throw new AuthenticationException(msg);
            }

            is = conn.getInputStream();
            return inputStreamToString(is);
        }
        finally {
            if(is != null){
                is.close();
            }
        }
    }

    private static String inputStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is, StandardCharsets.UTF_8.name()).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static void setXmsClientTelemetryInfo(final HttpsURLConnection conn, HttpEvent httpEvent){
        if(!StringHelper.isBlank(conn.getHeaderField("x-ms-request-id"))){
            httpEvent.setRequestIdHeader(conn.getHeaderField("x-ms-request-id"));
        }

        if(!StringHelper.isBlank(conn.getHeaderField("x-ms-clitelem"))){
            XmsClientTelemetryInfo xmsClientTelemetryInfo =
                    XmsClientTelemetryInfo.parseXmsTelemetryInfo(
                            conn.getHeaderField("x-ms-clitelem"));
            if(xmsClientTelemetryInfo != null){
                httpEvent.setXmsClientTelemetryInfo(xmsClientTelemetryInfo);
            }
        }
    }
}