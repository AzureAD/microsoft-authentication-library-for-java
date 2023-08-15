// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * Contains information about outgoing HTTP request. Should be adapted to HTTP request for HTTP
 * client of choice
 */
@Getter
@Accessors(fluent = true)
public class HttpRequest {

    /**
     * {@link HttpMethod}
     */
    private HttpMethod httpMethod;

    /**
     * HTTP request url
     */
    private URL url;

    /**
     * HTTP request headers
     */
    private Map<String, String> headers;

    /**
     * HTTP request body
     */
    private String body;

    HttpRequest(HttpMethod httpMethod, String url) {
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
    }

    HttpRequest(HttpMethod httpMethod, String url, Map<String, String> headers) {
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
        this.headers = headers;
    }

    HttpRequest(HttpMethod httpMethod, String url, String body) {
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
        this.body = body;
    }

    HttpRequest(HttpMethod httpMethod,
                String url, Map<String, String> headers,
                String body) {
        this.httpMethod = httpMethod;
        this.url = createUrlFromString(url);
        this.headers = headers;
        this.body = body;
    }

    /**
     * @param headerName Name of HTTP header name
     * @return Value of HTTP header
     */
    public String headerValue(String headerName) {

        if (headerName == null || headers == null) {
            return null;
        }

        return headers.get(headerName);
    }

    private URL createUrlFromString(String stringUrl) {
        URL url;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            throw new MsalClientException(e);
        }

        return url;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;

        if (!(o instanceof HttpRequest))
            return false;

        HttpRequest otherRequest = (HttpRequest) o;

        return this.url.equals(otherRequest.url)
                && this.httpMethod.equals(otherRequest.httpMethod)
                && ((this.headers == null && otherRequest.headers == null) || this.headers.equals(otherRequest.headers))
                && ((this.body == null && otherRequest.body == null) || this.body.equals(otherRequest.body));
    }
}
