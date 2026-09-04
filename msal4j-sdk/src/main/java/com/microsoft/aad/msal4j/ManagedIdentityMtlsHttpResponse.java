// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Response returned to the optional mTLS provider by MSAL's HTTP pipeline.
 */
public final class ManagedIdentityMtlsHttpResponse {

    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public ManagedIdentityMtlsHttpResponse(
            int statusCode,
            String body,
            Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers == null
                ? Collections.<String, List<String>>emptyMap()
                : Collections.unmodifiableMap(headers);
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }
}
