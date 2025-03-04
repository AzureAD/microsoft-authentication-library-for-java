// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.util.List;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class AadInstanceDiscoveryResponse implements JsonSerializable<AadInstanceDiscoveryResponse> {

    private String tenantDiscoveryEndpoint;
    private List<InstanceDiscoveryMetadataEntry> metadata;
    private String errorDescription;
    private List<Long> errorCodes;
    private String error;
    private String correlationId;

    public static AadInstanceDiscoveryResponse fromJson(JsonReader jsonReader) throws IOException {
        AadInstanceDiscoveryResponse response = new AadInstanceDiscoveryResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "tenant_discovery_endpoint":
                        response.tenantDiscoveryEndpoint = reader.getString();
                        break;
                    case "metadata":
                        response.metadata = reader.readArray(InstanceDiscoveryMetadataEntry::fromJson);
                        break;
                    case "error_description":
                        response.errorDescription = reader.getString();
                        break;
                    case "error_codes":
                        response.errorCodes = reader.readArray(JsonReader::getLong);
                        break;
                    case "error":
                        response.error = reader.getString();
                        break;
                    case "correlation_id":
                        response.correlationId = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return response;
        });
    }

    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("tenant_discovery_endpoint", tenantDiscoveryEndpoint);
        jsonWriter.writeArrayField("metadata", metadata, JsonWriter::writeJson);
        jsonWriter.writeStringField("error_description", errorDescription);
        jsonWriter.writeArrayField("error_codes", errorCodes, JsonWriter::writeLong);
        jsonWriter.writeStringField("error", error);
        jsonWriter.writeStringField("correlation_id", correlationId);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}