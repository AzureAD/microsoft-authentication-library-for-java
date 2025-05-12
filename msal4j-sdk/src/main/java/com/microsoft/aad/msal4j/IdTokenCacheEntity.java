// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class IdTokenCacheEntity extends Credential {

    private String credentialType;
    protected String realm;

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);
        keyParts.add(clientId);
        keyParts.add(realm);

        // target
        keyParts.add("");

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    static IdTokenCacheEntity fromJson(JsonReader jsonReader) throws IOException {
        IdTokenCacheEntity entity = new IdTokenCacheEntity();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "home_account_id":
                        entity.homeAccountId = reader.getString();
                        break;
                    case "environment":
                        entity.environment = reader.getString();
                        break;
                    case "credential_type":
                        entity.credentialType = reader.getString();
                        break;
                    case "client_id":
                        entity.clientId = reader.getString();
                        break;
                    case "secret":
                        entity.secret = reader.getString();
                        break;
                    case "realm":
                        entity.realm = reader.getString();
                        break;
                    case "user_assertion_hash":
                        entity.userAssertionHash = reader.getString();
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

        jsonWriter.writeStringField("home_account_id", homeAccountId);
        jsonWriter.writeStringField("environment", environment);
        jsonWriter.writeStringField("credential_type", credentialType);
        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeStringField("secret", secret);
        jsonWriter.writeStringField("realm", realm);
        jsonWriter.writeStringField("user_assertion_hash", userAssertionHash);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    String credentialType() {
        return this.credentialType;
    }

    String realm() {
        return this.realm;
    }

    void credentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    void realm(String realm) {
        this.realm = realm;
    }
}