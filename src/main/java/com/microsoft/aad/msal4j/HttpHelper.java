// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;

import javax.net.ssl.HttpsURLConnection;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

class HttpHelper {

    static String executeHttpGet(final Logger log, final String url,
                                 final ServiceBundle serviceBundle) throws Exception {
        return executeHttpGet(log, url, null, serviceBundle);
    }

    static String executeHttpGet(final Logger log, final String url,
                                 final Map<String, String> headers,
                                 final ServiceBundle serviceBundle) throws Exception {
        final HttpsURLConnection conn = HttpHelper.openConnection(url, serviceBundle);
        return executeGetRequest(log, headers, conn);
    }

    static String executeHttpPost(final Logger log, final String url,
            String postData, final ServiceBundle serviceBundle) throws Exception {
        return executeHttpPost(log, url, postData, null, serviceBundle);
    }

    static String executeHttpPost(final Logger log, final String url,
            String postData, final Map<String, String> headers,
            final ServiceBundle serviceBundle)
            throws Exception {
        final HttpsURLConnection conn = HttpHelper.openConnection(url, serviceBundle);
        return executePostRequest(log, postData, headers, conn);
    }

    static String inputStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    static String readResponseFromConnection(final HttpsURLConnection conn)
            throws AuthenticationException, IOException {
        InputStream is = null;
        try {
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                String msg = "Server returned HTTP response code: " + conn.getResponseCode() + " for URL : " +
                        conn.getURL();
                is = conn.getErrorStream();
                if (is != null) {
                    msg = msg + ", Error details : " + inputStreamToString(is);
                }
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

    static HttpsURLConnection openConnection(final String url, final ServiceBundle serviceBundle)
            throws IOException {
        return openConnection(new URL(url), serviceBundle);
    }

    static HttpsURLConnection configureAdditionalHeaders(
            final HttpsURLConnection conn, final Map<String, String> headers) {
        if (headers != null) {
            for (final Map.Entry<String, String> entry : headers.entrySet()) {
                conn.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        return conn;
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

    private static String executeGetRequest(Logger log,
            Map<String, String> headers, HttpsURLConnection conn)
            throws IOException {
        configureAdditionalHeaders(conn, headers);
        return getResponse(log, headers, conn);
    }

    private static String executePostRequest(Logger log, String postData,
            Map<String, String> headers, HttpsURLConnection conn)
            throws IOException {
        configureAdditionalHeaders(conn, headers);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        DataOutputStream wr = null;
        try {
            wr = new DataOutputStream(conn.getOutputStream());
            wr.writeBytes(postData);
            wr.flush();

            return getResponse(log, headers, conn);
        }
        finally {
            if (wr != null) {
                wr.close();
            }
        }
    }

    private static String getResponse(Logger log, Map<String, String> headers,
            HttpsURLConnection conn) throws IOException {
        String response = readResponseFromConnection(conn);
        if (headers != null) {
            HttpHelper.verifyReturnedCorrelationId(log, conn, headers
                    .get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
        }
        return response;
    }
}
