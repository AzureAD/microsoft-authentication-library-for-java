// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class AccessTokenCacheEntity extends Credential implements JsonSerializable<Credential> {

    private String credentialType;
    protected String realm;
    private String target;
    private String cachedAt;
    private String expiresOn;
    private String extExpiresOn;
    private String refreshOn;
    private String extCacheKeyHash;

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(StringHelper.isBlank(homeAccountId) ? "" : homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);
        keyParts.add(clientId);
        keyParts.add(realm);
        keyParts.add(target);

        if (!StringHelper.isBlank(extCacheKeyHash)) {
            keyParts.add(extCacheKeyHash);
        }

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    static AccessTokenCacheEntity fromJson(JsonReader jsonReader) throws IOException {
        AccessTokenCacheEntity entity = new AccessTokenCacheEntity();

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
                    case "target":
                        entity.target = reader.getString();
                        break;
                    case "cached_at":
                        entity.cachedAt = reader.getString();
                        break;
                    case "expires_on":
                        entity.expiresOn = reader.getString();
                        break;
                    case "extended_expires_on":
                        entity.extExpiresOn = reader.getString();
                        break;
                    case "refresh_on":
                        entity.refreshOn = reader.getString();
                        break;
                    case "user_assertion_hash":
                        entity.userAssertionHash = reader.getString();
                        break;
                    case "ext_cache_key_hash":
                        entity.extCacheKeyHash = reader.getString();
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
        jsonWriter.writeStringField("target", target);
        jsonWriter.writeStringField("cached_at", cachedAt);
        jsonWriter.writeStringField("expires_on", expiresOn);
        jsonWriter.writeStringField("extended_expires_on", extExpiresOn);
        jsonWriter.writeStringField("refresh_on", refreshOn);
        jsonWriter.writeStringField("user_assertion_hash", userAssertionHash);
        if (!StringHelper.isBlank(extCacheKeyHash)) {
            jsonWriter.writeStringField("ext_cache_key_hash", extCacheKeyHash);
        }

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    String target() {
        return this.target;
    }

    String cachedAt() {
        return this.cachedAt;
    }

    String expiresOn() {
        return this.expiresOn;
    }

    String extExpiresOn() {
        return this.extExpiresOn;
    }

    String refreshOn() {
        return this.refreshOn;
    }

    void credentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    void realm(String realm) {
        this.realm = realm;
    }

    void target(String target) {
        this.target = target;
    }

    void cachedAt(String cachedAt) {
        this.cachedAt = cachedAt;
    }

    void expiresOn(String expiresOn) {
        this.expiresOn = expiresOn;
    }

    void extExpiresOn(String extExpiresOn) {
        this.extExpiresOn = extExpiresOn;
    }

    void refreshOn(String refreshOn) {
        this.refreshOn = refreshOn;
    }

    String extCacheKeyHash() {
        return this.extCacheKeyHash;
    }

    void extCacheKeyHash(String extCacheKeyHash) {
        this.extCacheKeyHash = extCacheKeyHash;
    }
}