// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import net.minidev.json.JSONObject;
import com.nimbusds.oauth2.sdk.ParseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP response
 */
public class HttpResponse implements IHttpResponse {

    /**
     * HTTP response status code
     */
    private int statusCode;

    /**
     * HTTP response headers
     */
    private Map<String, List<String>> headers = new HashMap<>();

    /**
     * HTTP response body
     */
    private String body;

    /**
     * @param responseHeaders Map of HTTP headers returned from HTTP client
     */
    public void addHeaders(Map<String, List<String>> responseHeaders) {
        for (Map.Entry<String, List<String>> entry : responseHeaders.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }

            List<String> values = entry.getValue();
            if (values == null || values.isEmpty() || values.get(0) == null) {
                continue;
            }

            addHeader(entry.getKey(), values.toArray(new String[]{}));
        }
    }

    void addHeader(final String name, final String... values) {
        if (values != null && values.length > 0) {
            headers.put(name, Arrays.asList(values));
        } else {
            headers.remove(name);
        }
    }

    List<String> getHeader(String key) {
        return headers.get(key);
    }

    JSONObject getBodyAsJson() {
        try {
            return JSONObjectUtils.parse(this.body());
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    public int statusCode() {
        return this.statusCode;
    }

    public Map<String, List<String>> headers() {
        return this.headers;
    }

    public String body() {
        return this.body;
    }

    public HttpResponse statusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public HttpResponse body(String body) {
        this.body = body;
        return this;
    }
}
