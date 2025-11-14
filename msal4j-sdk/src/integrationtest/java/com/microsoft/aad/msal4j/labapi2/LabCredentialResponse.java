// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Response object for user credential requests from lab API.
 */
public class LabCredentialResponse implements JsonSerializable<LabCredentialResponse> {

    private String secret;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    /**
     * Deserialize a LabCredentialResponse from JSON.
     */
    public static LabCredentialResponse fromJson(JsonReader jsonReader) throws IOException {
        return jsonReader.readObject(reader -> {
            LabCredentialResponse response = new LabCredentialResponse();

            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                if ("secret".equals(fieldName)) {
                    response.secret = reader.getString();
                } else {
                    reader.skipChildren();
                }
            }
            return response;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("secret", secret);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}