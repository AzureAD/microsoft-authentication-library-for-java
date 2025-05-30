// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

enum HttpStatus {
    OK(200, "OK"),
    FOUND(302, "Found"),
    BAD_REQUEST(400, "Bad Request"),
    NOT_FOUND(404, "Not Found"),
    REQUEST_TIMEOUT(408, "Request Timeout"),
    GONE(410, "Gone"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
    SERVICE_UNAVAILABLE(503, "Service Unavailable"),
    GATEWAY_TIMEOUT(504, "Gateway Timeout");

    private final int code;
    private final String description;

    HttpStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    int getCode() {
        return code;
    }

    String getDescription() {
        return description;
    }

    //All 5xx errors
    static boolean isServerError(int code) {
        return code >= 500 && code < 600;
    }
}
