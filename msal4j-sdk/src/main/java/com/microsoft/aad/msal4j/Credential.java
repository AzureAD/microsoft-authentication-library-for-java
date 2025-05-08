// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

class Credential implements JsonSerializable<Credential> {

    protected String homeAccountId;
    protected String environment;
    protected String clientId;
    protected String secret;
    protected String userAssertionHash;

    static Credential fromJson(JsonReader jsonReader) throws IOException {
        Credential credential = new Credential();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "home_account_id":
                        credential.homeAccountId = reader.getString();
                        break;
                    case "environment":
                        credential.environment = reader.getString();
                        break;
                    case "client_id":
                        credential.clientId = reader.getString();
                        break;
                    case "secret":
                        credential.secret = reader.getString();
                        break;
                    case "user_assertion_hash":
                        credential.userAssertionHash = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return credential;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("home_account_id", homeAccountId);
        jsonWriter.writeStringField("environment", environment);
        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeStringField("secret", secret);
        jsonWriter.writeStringField("user_assertion_hash", userAssertionHash);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    String homeAccountId() {
        return this.homeAccountId;
    }

    String environment() {
        return this.environment;
    }

    String clientId() {
        return this.clientId;
    }

    String secret() {
        return this.secret;
    }

    String userAssertionHash() {
        return this.userAssertionHash;
    }

    void homeAccountId(String homeAccountId) {
        this.homeAccountId = homeAccountId;
    }

    void environment(String environment) {
        this.environment = environment;
    }

    void clientId(String clientId) {
        this.clientId = clientId;
    }

    void secret(String secret) {
        this.secret = secret;
    }

    void userAssertionHash(String userAssertionHash) {
        this.userAssertionHash = userAssertionHash;
    }
}