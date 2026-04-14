// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

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

    public HttpResponse send() throws IOException {

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

    /**
     * Sends the request to the given {@code overrideUrl} using the supplied {@code httpClient}
     * instead of the service bundle's default HTTP client.
     *
     * <p>Used for mTLS PoP flows where the token endpoint URL is the {@code mtlsauth.*} endpoint
     * and the HTTP client is configured with a client-certificate {@link javax.net.ssl.SSLSocketFactory}.</p>
     */
    HttpResponse sendWithClient(URL overrideUrl, IHttpClient httpClient) throws IOException {
        Map<String, String> httpHeaders = configureHttpHeaders();
        HttpRequest httpRequest = new HttpRequest(
                HttpMethod.POST,
                overrideUrl.toString(),
                httpHeaders,
                this.query);

        IHttpResponse httpResponse = ((HttpHelper) serviceBundle.getHttpHelper()).executeHttpRequest(
                httpRequest,
                this.requestContext,
                serviceBundle.getTelemetryManager(),
                httpClient);

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

    private HttpResponse createOauthHttpResponseFromHttpResponse(IHttpResponse httpResponse)
            throws IOException {

        final HttpResponse response = new HttpResponse();
        response.statusCode(httpResponse.statusCode());

        final String location = HttpUtils.headerValue(httpResponse.headers(), "Location");
        if (!StringHelper.isBlank(location)) {
            try {
                response.addHeader("Location", new URI(location).toString());
            } catch (URISyntaxException e) {
                throw new IOException("Invalid location URI " + location, e);
            }
        }

        String contentType = HttpUtils.headerValue(httpResponse.headers(), "Content-Type");
        if (!StringHelper.isBlank(contentType)) {
            response.addHeader("Content-Type", contentType);
        }

        Map<String, List<String>> headers = httpResponse.headers();
        for (Map.Entry<String, List<String>> header : headers.entrySet()) {

            if (StringHelper.isBlank(header.getKey())) {
                continue;
            }

            List<String> headerValue = response.getHeader((header.getKey()));
            if (headerValue == null) {
                response.addHeader(header.getKey(), header.getValue().toArray(new String[0]));
            }
        }

        if (!StringHelper.isBlank(httpResponse.body())) {
            response.body(httpResponse.body());
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
