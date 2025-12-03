// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Represents a JSON response of Lab information.
 */
public class LabConfig implements JsonSerializable<LabConfig> {
    private String labName;
    private String domain;
    private String tenantId;
    private String federationProvider;
    private String azureEnvironment;
    private String authority;

    static LabConfig fromJson(JsonReader jsonReader) throws IOException {
        LabConfig labConfig = new LabConfig();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "labName":
                        labConfig.labName = reader.getString();
                        break;
                    case "domain":
                        labConfig.domain = reader.getString();
                        break;
                    case "tenantId":
                        labConfig.tenantId = reader.getString();
                        break;
                    case "federationProvider":
                        labConfig.federationProvider = reader.getString();
                        break;
                    case "azureEnvironment":
                        labConfig.azureEnvironment = reader.getString();
                        break;
                    case "authority":
                        labConfig.authority = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return labConfig;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("labName", labName);
        jsonWriter.writeStringField("domain", domain);
        jsonWriter.writeStringField("tenantId", tenantId);
        jsonWriter.writeStringField("federationProvider", federationProvider);
        jsonWriter.writeStringField("azureEnvironment", azureEnvironment);
        jsonWriter.writeStringField("authority", authority);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    public String getTenantId() {
        return this.tenantId;
    }
}