// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Container that represents JSON responses from our test infrastructure, such as user and app config needed by integration tests
 */
public class LabResponse implements JsonSerializable<LabResponse> {

    private UserConfig user;
    private AppConfig app;

    public UserConfig getUser() {
        return user;
    }

    public AppConfig getApp() {
        return app;
    }

    /**
     * Deserialize a LabResponse from JSON.
     */
    static LabResponse fromJson(JsonReader jsonReader) throws IOException {
        return jsonReader.readObject(reader -> {
            LabResponse response = new LabResponse();

            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName().toLowerCase();
                reader.nextToken();

                switch (fieldName) {
                    case "user":
                        response.user = UserConfig.fromJson(reader);
                        break;
                    case "app":
                        response.app = AppConfig.fromJson(reader);
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

        if (user != null) {
            jsonWriter.writeJsonField("user", user);
        }
        if (app != null) {
            jsonWriter.writeJsonField("app", app);
        }

        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}