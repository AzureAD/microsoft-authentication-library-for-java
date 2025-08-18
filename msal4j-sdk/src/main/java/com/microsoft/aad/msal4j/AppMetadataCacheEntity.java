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

class AppMetadataCacheEntity implements JsonSerializable<AppMetadataCacheEntity> {

    public static final String APP_METADATA_CACHE_ENTITY_ID = "appmetadata";

    private String clientId;
    private String environment;
    private String familyId;

    static AppMetadataCacheEntity fromJson(JsonReader jsonReader) throws IOException {
        AppMetadataCacheEntity entity = new AppMetadataCacheEntity();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "client_id":
                        entity.clientId = reader.getString();
                        break;
                    case "environment":
                        entity.environment = reader.getString();
                        break;
                    case "family_id":
                        entity.familyId = reader.getString();
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

        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeStringField( "environment", environment);
        jsonWriter.writeStringField("family_id", familyId);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(APP_METADATA_CACHE_ENTITY_ID);
        keyParts.add(environment);
        keyParts.add(clientId);

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    String clientId() {
        return this.clientId;
    }

    String environment() {
        return this.environment;
    }

    String familyId() {
        return this.familyId;
    }

    void clientId(String clientId) {
        this.clientId = clientId;
    }

    void environment(String environment) {
        this.environment = environment;
    }

    void familyId(String familyId) {
        this.familyId = familyId;
    }
}