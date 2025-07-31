// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * TODO: Add class description
 */
public class ManagedIdentityErrorResponse {

    @JsonProperty("message")
    private String message;

    @JsonProperty("correlationId")
    private String correlationId;

    //In some MSI scenarios such as Cloud Shell, the actual error info is in a JSON within the main JSON. To parse that second
    // JSON layer, we need to first pass it into a subclass, parse it using the usual @JsonProperty annotation, and then retrieve the values.
    @JsonProperty("error")
    private void parseErrorField(ErrorField errorResponse) {
        this.error = errorResponse.code;
        this.message = errorResponse.message;
    }

    @JsonProperty("error")
    private String error;

    @JsonProperty("error_description")
    private String errorDescription;

    /**
     * Gets the message.
     * 
     * @return the message
     */
    public String getMessage() {
        return this.message;
    }

    /**
     * Gets the correlation id.
     * 
     * @return the correlation id
     */
    public String getCorrelationId() {
        return this.correlationId;
    }

    /**
     * Gets the error.
     * 
     * @return the error
     */
    public String getError() {
        return this.error;
    }

    /**
     * Gets the error description.
     * 
     * @return the error description
     */
    public String getErrorDescription() {
        return this.errorDescription;
    }

    private static class ErrorField {
        @JsonProperty("code")
        private String code;

        @JsonProperty("message")
        private String message;

       String getCode() {
            return this.code;
        }

        String getMessage() {
            return this.message;
        }
    }
}
