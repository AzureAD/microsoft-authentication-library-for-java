// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

class OAuthHttpRequest extends HTTPRequest {

    private final Map<String, String> extraHeaderParams;
    private final Logger log = LoggerFactory.getLogger(OAuthHttpRequest.class);
    private final ServiceBundle serviceBundle;

    OAuthHttpRequest(final Method method, final URL url,
                     final Map<String, String> extraHeaderParams, final ServiceBundle serviceBundle) {
        super(method, url);
        this.extraHeaderParams = extraHeaderParams;
        this.serviceBundle = serviceBundle;
    }

    Map<String, String> getReadOnlyExtraHeaderParameters() {
        return Collections.unmodifiableMap(this.extraHeaderParams);
    }

    @Override
    public HTTPResponse send() throws IOException {

        final HttpsURLConnection conn = HttpHelper.openConnection(this.getURL(),
                this.serviceBundle);
        this.configureHeaderAndExecuteOAuthCall(conn);
        final String out = this.processAndReadResponse(conn);
        HttpHelper.verifyReturnedCorrelationId(log, conn,
                this.extraHeaderParams.get(ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
        return createResponse(conn, out);
    }

    private HTTPResponse createResponse(final HttpURLConnection conn, final String out)
            throws IOException {

        final HTTPResponse response = new HTTPResponse(conn.getResponseCode());

        final String location = conn.getHeaderField("Location");
        if (!StringHelper.isBlank(location)) {
            try {
                response.setLocation(new URI(location));
            } catch (URISyntaxException e) {
                throw new IOException("Invalid location URI " + location, e);
            }
        }

        try {
            response.setContentType(conn.getContentType());
        }
        catch (final ParseException e) {
            throw new IOException("Couldn't parse Content-Type header: "
                    + e.getMessage(), e);
        }

        Map<String, List<String>> headers = conn.getHeaderFields();
        for(Map.Entry<String, List<String>> header: headers.entrySet()){

            if(StringHelper.isBlank(header.getKey())){
                continue;
            }

            String headerValue = response.getHeaderValue(header.getKey());
            if(headerValue == null || StringHelper.isBlank(headerValue)){
                response.setHeader(header.getKey(), header.getValue().toArray(new String[0]));
            }
        }

        if (!StringHelper.isBlank(out)) {
            response.setContent(out);
        }
        return response;
    }

    private void configureHeaderAndExecuteOAuthCall(final HttpsURLConnection conn)
            throws IOException {

        if (this.getAuthorization() != null) {
            conn.setRequestProperty("Authorization", this.getAuthorization());
        }

        Map<String, String> params = new java.util.HashMap<>();
        if (this.extraHeaderParams != null && !this.extraHeaderParams.isEmpty()) {
            for (java.util.Map.Entry<String, String> entry : this.extraHeaderParams
                    .entrySet()) {
                if (entry.getValue() == null || entry.getValue().isEmpty()) {
                    continue;
                }
                params.put(entry.getKey(), entry.getValue());
            }
        }

        HttpHelper.configureAdditionalHeaders(conn, params, serviceBundle);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", CommonContentTypes.APPLICATION_URLENCODED.toString());

        if (this.getQuery() != null) {
            try(final OutputStreamWriter writer = new OutputStreamWriter(
                    conn.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(getQuery());
                writer.flush();
            }
        }
    }

    private String processAndReadResponse(final HttpURLConnection conn)
            throws IOException {
        Reader inReader;
        final int responseCode = conn.getResponseCode();
        if (responseCode == 200) {
            inReader = new InputStreamReader(conn.getInputStream(),  StandardCharsets.UTF_8);
        }
        else {
            InputStream stream = conn.getErrorStream();
            if (stream == null && responseCode == 404) {
                stream = conn.getInputStream();
            }

            if (stream == null) {
                stream = conn.getInputStream();
            }

            inReader = new InputStreamReader(stream, StandardCharsets.UTF_8);
        }
        final BufferedReader reader = new BufferedReader(inReader);
        final char[] buffer = new char[256];
        final StringBuilder out = new StringBuilder();
        try {
            for (;;) {
                final int rsz = reader.read(buffer, 0, buffer.length);
                if (rsz < 0) {
                    break;
                }
                out.append(buffer, 0, rsz);
            }
        }
        finally {
            reader.close();
        }
        return out.toString();
    }
}