// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

class ClientInfo implements JsonSerializable<ClientInfo> {

    private String uniqueIdentifier;
    private String uniqueTenantIdentifier;

    public static ClientInfo createFromJson(String clientInfoJsonBase64Encoded) {
        if (StringHelper.isBlank(clientInfoJsonBase64Encoded)) {
            return null;
        }

        byte[] decodedInput = Base64.getUrlDecoder().decode(clientInfoJsonBase64Encoded.getBytes(StandardCharsets.UTF_8));

        return JsonHelper.convertJsonStringToJsonSerializableObject(new String(decodedInput, StandardCharsets.UTF_8), ClientInfo::fromJson);
    }

    static ClientInfo fromJson(JsonReader jsonReader) throws IOException {
        ClientInfo clientInfo = new ClientInfo();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "uid":
                        clientInfo.uniqueIdentifier = reader.getString();
                        break;
                    case "utid":
                        clientInfo.uniqueTenantIdentifier = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return clientInfo;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("uid", uniqueIdentifier);
        jsonWriter.writeStringField("utid", uniqueTenantIdentifier);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    String toAccountIdentifier() {
        return uniqueIdentifier + POINT_DELIMITER + uniqueTenantIdentifier;
    }

    String getUniqueIdentifier() {
        return this.uniqueIdentifier;
    }

    String getUniqueTenantIdentifier() {
        return this.uniqueTenantIdentifier;
    }
}