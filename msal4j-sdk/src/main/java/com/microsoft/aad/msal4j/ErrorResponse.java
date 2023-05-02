// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter
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
}
