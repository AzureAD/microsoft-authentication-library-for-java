// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;

class ErrorResponse {

    private Integer statusCode;
    private String statusMessage;

    @JsonProperty("error")
    protected String error;

    @JsonProperty("error_description")
    protected String errorDescription;

    @JsonProperty("error_codes")
    protected long[] errorCodes;

    @JsonProperty("suberror")
    protected String subError;

    @JsonProperty("trace_id")
    protected String traceId;

    @JsonProperty("timestamp")
    protected String timestamp;

    @JsonProperty("correlation_id")
    protected String correlation_id;

    @JsonProperty("claims")
    private String claims;

    Integer statusCode() {
        return this.statusCode;
    }

    String statusMessage() {
        return this.statusMessage;
    }

    String error() {
        return this.error;
    }

    String errorDescription() {
        return this.errorDescription;
    }

    long[] errorCodes() {
        return this.errorCodes;
    }

    String subError() {
        return this.subError;
    }

    String traceId() {
        return this.traceId;
    }

    String timestamp() {
        return this.timestamp;
    }

    String correlation_id() {
        return this.correlation_id;
    }

    String claims() {
        return this.claims;
    }

    void statusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    void statusMessage(String statusMessage) {
        this.statusMessage = statusMessage;
    }

    void error(String error) {
        this.error = error;
    }

    void errorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    void errorCodes(long[] errorCodes) {
        this.errorCodes = errorCodes;
    }

    void subError(String subError) {
        this.subError = subError;
    }

    void traceId(String traceId) {
        this.traceId = traceId;
    }

    void timestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    void correlation_id(String correlation_id) {
        this.correlation_id = correlation_id;
    }

    void claims(String claims) {
        this.claims = claims;
    }
}
