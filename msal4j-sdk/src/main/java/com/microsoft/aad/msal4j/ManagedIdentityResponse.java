// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

class ManagedIdentityResponse implements JsonSerializable<ManagedIdentityResponse> {

    String tokenType;
    String accessToken;
    String expiresOn;
    String resource;
    String clientId;

    public static ManagedIdentityResponse fromJson(JsonReader jsonReader) throws IOException {
        ManagedIdentityResponse response = new ManagedIdentityResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "token_type":
                        response.tokenType = reader.getString();
                        break;
                    case "access_token":
                        response.accessToken = reader.getString();
                        break;
                    case "expires_on":
                        response.expiresOn = reader.getString();
                        break;
                    case "resource":
                        response.resource = reader.getString();
                        break;
                    case "client_id":
                        response.clientId = reader.getString();
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
        jsonWriter.writeStringField("token_type", tokenType);
        jsonWriter.writeStringField("access_token", accessToken);
        jsonWriter.writeStringField("expires_on", expiresOn);
        jsonWriter.writeStringField("resource", resource);
        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    public String getTokenType() {
        return this.tokenType;
    }

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getExpiresOn() {
        return this.expiresOn;
    }

    public String getResource() {
        return this.resource;
    }

    public String getClientId() {
        return this.clientId;
    }
}