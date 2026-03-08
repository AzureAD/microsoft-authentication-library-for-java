// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * DTO containing platform metadata returned by the IMDS
 * {@code GET /metadata/identity/getPlatformMetadata} endpoint.
 * Used in the MSI v2 mTLS PoP flow.
 */
class CsrMetadata implements JsonSerializable<CsrMetadata> {

    String clientId;
    String tenantId;
    String cuId;
    String attestationEndpoint;

    public static CsrMetadata fromJson(JsonReader jsonReader) throws IOException {
        CsrMetadata metadata = new CsrMetadata();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "client_id":
                        metadata.clientId = reader.getString();
                        break;
                    case "tenant_id":
                        metadata.tenantId = reader.getString();
                        break;
                    case "cu_id":
                        metadata.cuId = reader.getString();
                        break;
                    case "attestation_endpoint":
                        metadata.attestationEndpoint = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return metadata;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("client_id", clientId);
        jsonWriter.writeStringField("tenant_id", tenantId);
        jsonWriter.writeStringField("cu_id", cuId);
        jsonWriter.writeStringField("attestation_endpoint", attestationEndpoint);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}
