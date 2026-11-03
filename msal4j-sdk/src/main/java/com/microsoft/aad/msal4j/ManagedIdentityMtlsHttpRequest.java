// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Immutable IMDS request issued through MSAL's HTTP, retry, proxy and telemetry pipeline.
 */
public final class ManagedIdentityMtlsHttpRequest {

    private final String method;
    private final String url;
    private final Map<String, String> headers;
    private final String body;
    private final boolean followRedirects;

    public ManagedIdentityMtlsHttpRequest(
            String method,
            String url,
            Map<String, String> headers,
            String body) {
        this(method, url, headers, body, true);
    }

    public ManagedIdentityMtlsHttpRequest(
            String method,
            String url,
            Map<String, String> headers,
            String body,
            boolean followRedirects) {
        this.method = method;
        this.url = url;
        this.headers = headers == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body;
        this.followRedirects = followRedirects;
    }

    public String method() {
        return method;
    }

    public String url() {
        return url;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String body() {
        return body;
    }

    public boolean followRedirects() {
        return followRedirects;
    }
}
