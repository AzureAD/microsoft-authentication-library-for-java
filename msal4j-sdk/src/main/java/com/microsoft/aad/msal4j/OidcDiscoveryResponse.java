// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

class OidcDiscoveryResponse implements JsonSerializable<OidcDiscoveryResponse> {

    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String deviceCodeEndpoint;
    private String issuer;

    public static OidcDiscoveryResponse fromJson(JsonReader jsonReader) throws IOException {
        OidcDiscoveryResponse response = new OidcDiscoveryResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "authorization_endpoint":
                        response.authorizationEndpoint = reader.getString();
                        break;
                    case "token_endpoint":
                        response.tokenEndpoint = reader.getString();
                        break;
                    case "device_authorization_endpoint":
                        response.deviceCodeEndpoint = reader.getString();
                        break;
                    case "issuer":
                        response.issuer = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return response;
        });
    }

    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("issuer", issuer);
        jsonWriter.writeStringField("authorization_endpoint", authorizationEndpoint);
        jsonWriter.writeStringField("token_endpoint", tokenEndpoint);
        jsonWriter.writeStringField("device_authorization_endpoint", deviceCodeEndpoint);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    String authorizationEndpoint() {
        return this.authorizationEndpoint;
    }

    String tokenEndpoint() {
        return this.tokenEndpoint;
    }

    String deviceCodeEndpoint() {
        return this.deviceCodeEndpoint;
    }

    String issuer() {
        return this.issuer;
    }
}
