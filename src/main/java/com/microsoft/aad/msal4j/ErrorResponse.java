// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter
class ErrorResponse {

    private Integer statusCode;
    private String statusMessage;

    @SerializedName("error")
    protected String error;

    @SerializedName("error_description")
    protected String errorDescription;

    @SerializedName("error_codes")
    protected long[] errorCodes;

    @SerializedName("suberror")
    protected String subError;

    @SerializedName("trace_id")
    protected String traceId;

    @SerializedName("timestamp")
    protected String timestamp;

    @SerializedName("correlation_id")
    protected String correlation_id;

    @SerializedName("claims")
    private String claims;
}
