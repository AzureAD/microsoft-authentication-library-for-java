// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

public class ManagedIdentityErrorResponse implements JsonSerializable<ManagedIdentityErrorResponse> {

    private String message;
    private String correlationId;
    private String error;
    private String errorDescription;

    public static ManagedIdentityErrorResponse fromJson(JsonReader jsonReader) throws IOException {
        ManagedIdentityErrorResponse response = new ManagedIdentityErrorResponse();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "message":
                        response.message = reader.getString();
                        break;
                    case "correlationId":
                        response.correlationId = reader.getString();
                        break;
                    case "error":
                        if (reader.currentToken() == JsonToken.START_OBJECT) {
                            // Handle nested JSON error object
                            ErrorField errorField = ErrorField.fromJson(reader);
                            response.error = errorField.getCode();
                            response.message = errorField.getMessage();
                        } else {
                            response.error = reader.getString();
                        }
                        break;
                    case "error_description":
                        response.errorDescription = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return response;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("message", message);
        jsonWriter.writeStringField("correlationId", correlationId);
        jsonWriter.writeStringField("error", error);
        jsonWriter.writeStringField("error_description", errorDescription);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    public String getMessage() {
        return this.message;
    }

    public String getCorrelationId() {
        return this.correlationId;
    }

    public String getError() {
        return this.error;
    }

    public String getErrorDescription() {
        return this.errorDescription;
    }

    private static class ErrorField {
        private String code;
        private String message;

        static ErrorField fromJson(JsonReader jsonReader) throws IOException {
            ErrorField errorField = new ErrorField();

            return jsonReader.readObject(reader -> {
                while (reader.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = reader.getFieldName();
                    reader.nextToken();

                    switch (fieldName) {
                        case "code":
                            errorField.code = reader.getString();
                            break;
                        case "message":
                            errorField.message = reader.getString();
                            break;
                        default:
                            reader.skipChildren();
                            break;
                    }
                }
                return errorField;
            });
        }

        String getCode() {
            return this.code;
        }

        String getMessage() {
            return this.message;
        }
    }
}