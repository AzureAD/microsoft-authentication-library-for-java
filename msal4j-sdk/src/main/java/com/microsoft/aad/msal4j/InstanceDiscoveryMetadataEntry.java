// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

class InstanceDiscoveryMetadataEntry implements JsonSerializable<InstanceDiscoveryMetadataEntry> {

    String preferredNetwork;
    String preferredCache;
    Set<String> aliases;

    public InstanceDiscoveryMetadataEntry(String preferredNetwork, String preferredCache, Set<String> aliases) {
        this.preferredNetwork = preferredNetwork;
        this.preferredCache = preferredCache;
        this.aliases = aliases;
    }

    public InstanceDiscoveryMetadataEntry() {
    }

    /**
     * TODO: Add description
     */
    public static InstanceDiscoveryMetadataEntry fromJson(JsonReader jsonReader) throws IOException {
        InstanceDiscoveryMetadataEntry entry = new InstanceDiscoveryMetadataEntry();
        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();
                switch (fieldName) {
                    case "preferred_network":
                        entry.preferredNetwork = reader.getString();
                        break;
                    case "preferred_cache":
                        entry.preferredCache = reader.getString();
                        break;
                    case "aliases":
                        entry.aliases = new HashSet<>(reader.readArray(JsonReader::getString));
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return entry;
        });
    }

    /**
     * TODO: Add description
     */
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        jsonWriter.writeStringField("preferred_network", preferredNetwork);
        jsonWriter.writeStringField("preferred_cache", preferredCache);
        jsonWriter.writeArrayField("aliases", aliases, JsonWriter::writeString);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    String preferredNetwork() {
        return this.preferredNetwork;
    }

    String preferredCache() {
        return this.preferredCache;
    }

    Set<String> aliases() {
        return this.aliases;
    }
}
