// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

/**
 * HTTP response returned by {@link MtlsMsiClient#httpRequest} when making a
 * downstream mTLS call through {@code MsalMtlsMsiHelper.exe --mode http-request}.
 */
public class MtlsMsiHttpResponse {

    private final int status;
    private final String body;
    private final String rawJson;

    MtlsMsiHttpResponse(int status, String body, String rawJson) {
        this.status = status;
        this.body = body;
        this.rawJson = rawJson;
    }

    /** HTTP status code (e.g. 200, 401). */
    public int getStatus() { return status; }

    /** Response body as a string. */
    public String getBody() { return body; }

    /** Raw JSON string from the helper subprocess (contains status, headers, body). */
    public String getRawJson() { return rawJson; }
}
