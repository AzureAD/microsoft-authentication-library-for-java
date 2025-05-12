// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

class ErrorResponse implements JsonSerializable<ErrorResponse> {

    private Integer statusCode;
    private String statusMessage;
    protected String error;
    protected String errorDescription;
    protected long[] errorCodes;
    protected String subError;
    protected String traceId;
    protected String timestamp;
    protected String correlation_id;
    private String claims;

    static ErrorResponse fromJson(JsonReader jsonReader) throws IOException {
        ErrorResponse entity = new ErrorResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "error":
                        entity.error = reader.getString();
                        break;
                    case "error_description":
                        entity.errorDescription = reader.getString();
                        break;
                    case "error_codes":
                        entity.errorCodes = reader.readArray(JsonReader::getLong).stream().mapToLong(Long::longValue).toArray();
                        break;
                    case "suberror":
                        entity.subError = reader.getString();
                        break;
                    case "trace_id":
                        entity.traceId = reader.getString();
                        break;
                    case "timestamp":
                        entity.timestamp = reader.getString();
                        break;
                    case "correlation_id":
                        entity.correlation_id = reader.getString();
                        break;
                    case "claims":
                        entity.claims = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return entity;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStartObject();

        jsonWriter.writeNumberField("statusCode", statusCode);
        jsonWriter.writeStringField("statusMessage", statusMessage);
        jsonWriter.writeStringField("error", error);
        jsonWriter.writeStringField("error_description", errorDescription);

        if (errorCodes != null) {
            jsonWriter.writeStartArray("error_codes");
            for (long code : errorCodes) {
                jsonWriter.writeNumber(code);
            }
            jsonWriter.writeEndArray();
        } else {
            jsonWriter.writeNullField("error_codes");
        }

        jsonWriter.writeStringField("suberror", subError);
        jsonWriter.writeStringField("trace_id", traceId);
        jsonWriter.writeStringField("timestamp", timestamp);
        jsonWriter.writeStringField("correlation_id", correlation_id);
        jsonWriter.writeStringField("claims", claims);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

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