// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Container for lab API response data containing user, app, and lab information.
 */
public class LabResponse implements JsonSerializable<LabResponse> {

    private LabUser user;
    private LabApp app;
    private Lab lab;

    public LabUser getUser() {
        return user;
    }

    void setUser(LabUser user) {
        this.user = user;
    }

    public LabApp getApp() {
        return app;
    }

    void setApp(LabApp app) {
        this.app = app;
    }

    public Lab getLab() {
        return lab;
    }

    void setLab(Lab lab) {
        this.lab = lab;
    }

    /**
     * Deserialize a LabResponse from JSON.
     */
    static LabResponse fromJson(JsonReader jsonReader) throws IOException {
        return jsonReader.readObject(reader -> {
            LabResponse response = new LabResponse();

            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "user":
                        response.user = LabUser.fromJson(reader);
                        break;
                    case "app":
                        response.app = LabApp.fromJson(reader);
                        break;
                    case "lab":
                        response.lab = Lab.fromJson(reader);
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
        if (lab != null) {
            jsonWriter.writeJsonField("lab", lab);
        }

        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    @Override
    public String toString() {
        return String.format("LabResponse{user=%s, app=%s, lab=%s}",
                user != null ? user.getUpn() : "null",
                app != null ? app.getAppId() : "null",
                lab != null ? lab.getTenantId() : "null");
    }
}