// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

/**
 * Response body returned by the IMDS compute metadata endpoint
 * (http://169.254.169.254/metadata/instance/compute). Only the {@code location}
 * field is used for region auto-discovery; all other fields are ignored.
 */
class ImdsComputeResponse implements JsonSerializable<ImdsComputeResponse> {

    String location;

    public ImdsComputeResponse() {
    }

    String location() {
        return this.location;
    }

    public static ImdsComputeResponse fromJson(JsonReader jsonReader) throws IOException {
        ImdsComputeResponse response = new ImdsComputeResponse();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                if ("location".equals(fieldName)) {
                    response.location = reader.getString();
                } else {
                    reader.skipChildren();
                }
            }
            return response;
        });
    }

    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("location", location);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }
}
