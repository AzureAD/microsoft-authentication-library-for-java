// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

public class Lab implements JsonSerializable<Lab> {
    private String labName;
    private String domain;
    private String tenantId;
    private String federationProvider;
    private String azureEnvironment;
    private String authority;

    static Lab fromJson(JsonReader jsonReader) throws IOException {
        Lab lab = new Lab();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "labName":
                        lab.labName = reader.getString();
                        break;
                    case "domain":
                        lab.domain = reader.getString();
                        break;
                    case "tenantId":
                        lab.tenantId = reader.getString();
                        break;
                    case "federationProvider":
                        lab.federationProvider = reader.getString();
                        break;
                    case "azureEnvironment":
                        lab.azureEnvironment = reader.getString();
                        break;
                    case "authority":
                        lab.authority = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return lab;
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