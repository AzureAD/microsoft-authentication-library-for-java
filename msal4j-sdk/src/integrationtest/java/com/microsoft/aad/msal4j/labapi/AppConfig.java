// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Represents a JSON response describing an Azure app registration.
 */
public class AppConfig implements JsonSerializable<AppConfig> {

    private String appType;
    private String appName;
    private String appId;
    private String redirectUri;
    private String authority;
    private String labName;
    private String clientSecret;
    private String secretName;
    private String defaultScopes;

    static AppConfig fromJson(JsonReader jsonReader) throws IOException {
        AppConfig app = new AppConfig();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "appType":
                        app.appType = reader.getString();
                        break;
                    case "appName":
                        app.appName = reader.getString();
                        break;
                    case "appId":
                        app.appId = reader.getString();
                        break;
                    case "redirectUri":
                        app.redirectUri = reader.getString();
                        break;
                    case "authority":
                        app.authority = reader.getString();
                        break;
                    case "labName":
                        app.labName = reader.getString();
                        break;
                    case "clientSecret":
                        app.clientSecret = reader.getString();
                        break;
                    case "secretName":
                        app.secretName = reader.getString();
                        break;
                    case "defaultscopes":
                    case "defaultScopes":
                        app.defaultScopes = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return app;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("appType", appType);
        jsonWriter.writeStringField("appName", appName);
        jsonWriter.writeStringField("appId", appId);
        jsonWriter.writeStringField("redirectUri", redirectUri);
        jsonWriter.writeStringField("authority", authority);
        jsonWriter.writeStringField("labName", labName);
        jsonWriter.writeStringField("clientSecret", clientSecret);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    public String getAuthority() {
        return authority;
    }

    public String getAppId() {
        return appId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getSecretName() {
        return secretName;
    }

    public String getDefaultScopes() {
        return defaultScopes;
    }
}