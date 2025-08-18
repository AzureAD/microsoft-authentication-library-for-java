// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class RefreshTokenCacheEntity extends Credential {

    private String credentialType;
    private String family_id;

    static RefreshTokenCacheEntity fromJson(JsonReader jsonReader) throws IOException {
        RefreshTokenCacheEntity entity = new RefreshTokenCacheEntity();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "credential_type":
                        entity.credentialType = reader.getString();
                        break;
                    case "family_id":
                        entity.family_id = reader.getString();
                        break;
                    // Include other fields from the Credential parent class
                    case "home_account_id":
                        entity.homeAccountId = reader.getString();
                        break;
                    case "environment":
                        entity.environment = reader.getString();
                        break;
                    case "client_id":
                        entity.clientId = reader.getString();
                        break;
                    case "secret":
                        entity.secret = reader.getString();
                        break;
                    // Add other Credential fields as needed
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

        jsonWriter.writeStringField("credential_type", credentialType);
        jsonWriter.writeStringField("family_id", family_id);
        jsonWriter.writeStringField("home_account_id", homeAccountId);
        jsonWriter.writeStringField("environment", environment);
        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeStringField("secret", secret);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    boolean isFamilyRT() {
        return !StringHelper.isBlank(family_id);
    }

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);

        if (isFamilyRT()) {
            keyParts.add(family_id);
        } else {
            keyParts.add(clientId);
        }

        // realm
        keyParts.add("");
        // target
        keyParts.add("");

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    String credentialType() {
        return this.credentialType;
    }

    String family_id() {
        return this.family_id;
    }

    void credentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    void family_id(String family_id) {
        this.family_id = family_id;
    }
}