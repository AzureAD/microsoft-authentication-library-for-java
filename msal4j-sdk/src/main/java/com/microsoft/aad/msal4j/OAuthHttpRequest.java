// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class OAuthHttpRequest {

    final HttpMethod method;
    final URL url;
    String query;
    private final Map<String, String> extraHeaderParams;
    private final ServiceBundle serviceBundle;
    private final RequestContext requestContext;

    OAuthHttpRequest(final HttpMethod method,
                     final URL url,
                     final Map<String, String> extraHeaderParams,
                     RequestContext requestContext,
                     final ServiceBundle serviceBundle) {
        this.method = method;
        this.url = url;
        this.extraHeaderParams = extraHeaderParams;
        this.requestContext = requestContext;
        this.serviceBundle = serviceBundle;
    }

    public HTTPResponse send() throws IOException {

        Map<String, String> httpHeaders = configureHttpHeaders();
        HttpRequest httpRequest = new HttpRequest(
                HttpMethod.POST,
                this.url.toString(),
                httpHeaders,
                this.query);

        IHttpResponse httpResponse = serviceBundle.getHttpHelper().executeHttpRequest(
                httpRequest,
                this.requestContext,
                this.serviceBundle);

        return createOauthHttpResponseFromHttpResponse(httpResponse);
    }

    private Map<String, String> configureHttpHeaders() {

        Map<String, String> httpHeaders = new HashMap<>(extraHeaderParams);
        httpHeaders.put("Content-Type", HTTPContentType.ApplicationURLEncoded.contentType);

        Map<String, String> telemetryHeaders =
                serviceBundle.getServerSideTelemetry().getServerTelemetryHeaderMap();
        httpHeaders.putAll(telemetryHeaders);

        return httpHeaders;
    }

    private HTTPResponse createOauthHttpResponseFromHttpResponse(IHttpResponse httpResponse)
            throws IOException {

        final HTTPResponse response = new HTTPResponse(httpResponse.statusCode());

        final String location = HttpUtils.headerValue(httpResponse.headers(), "Location");
        if (!StringHelper.isBlank(location)) {
            try {
                response.setLocation(new URI(location));
            } catch (URISyntaxException e) {
                throw new IOException("Invalid location URI " + location, e);
            }
        }

        try {
            String contentType = HttpUtils.headerValue(httpResponse.headers(), "Content-Type");
            if (!StringHelper.isBlank(contentType)) {
                response.setContentType(contentType);
            }
        } catch (final ParseException e) {
            throw new IOException("Couldn't parse Content-Type header: "
                    + e.getMessage(), e);
        }

        Map<String, List<String>> headers = httpResponse.headers();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {

            if (StringHelper.isBlank(header.getKey())) {
                continue;
            }

            String headerValue = response.getHeaderValue(header.getKey());
            if (headerValue == null || StringHelper.isBlank(headerValue)) {
                response.setHeader(header.getKey(), header.getValue().toArray(new String[0]));
            }
        }

        if (!StringHelper.isBlank(httpResponse.body())) {
            response.setContent(httpResponse.body());
        }
        return response;
    }

    void setQuery(String query) {
        this.query = query;
    }

    Map<String, String> getExtraHeaderParams() {
        return this.extraHeaderParams;
    }
}
