// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public static ErrorResponse convertJsonToObject(String json) throws IOException {

        ErrorResponse errorResponse = new ErrorResponse();

        if (json != null) {
            JsonFactory jsonFactory = new JsonFactory();
            try (JsonParser jsonParser = jsonFactory.createParser(json)) {


                while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldname = jsonParser.getCurrentName();
                    if ("error".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.error = jsonParser.getText();
                    } else if ("error_description".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.errorDescription = jsonParser.getText();
                    } else if ("error_codes".equals(fieldname)) {
                        jsonParser.nextToken();
                        jsonParser.nextToken();
                        List<Long> errorCodesList = new ArrayList<>();
                        while (jsonParser.currentToken() != JsonToken.END_ARRAY) {
                            errorCodesList.add(jsonParser.getLongValue());
                            jsonParser.nextToken();
                        }
                        errorResponse.errorCodes = errorCodesList.stream().mapToLong(l -> l).toArray();
                    } else if ("suberror".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.subError = jsonParser.getText();
                    } else if ("trace_id".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.traceId = jsonParser.getText();
                    } else if ("timestamp".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.timestamp = jsonParser.getText();
                    } else if ("correlation_id".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.correlation_id = jsonParser.getText();
                    } else if ("claims".equals(fieldname)) {
                        jsonParser.nextToken();
                        errorResponse.claims = jsonParser.getText();
                    }

                }
            }
        }
        return errorResponse;
    }
}
