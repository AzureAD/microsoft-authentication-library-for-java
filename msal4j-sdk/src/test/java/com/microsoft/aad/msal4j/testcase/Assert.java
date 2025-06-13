// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Assert implements JsonSerializable<Assert> {
    private final Map<String, Object> assertions = new HashMap<>();

    public static Assert fromJson(JsonReader jsonReader) throws IOException {
        Assert assertion = new Assert();

        jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String key = reader.getFieldName();
                reader.nextToken();

                switch (reader.currentToken()) {
                    case STRING:
                        assertion.assertions.put(key, reader.getString());
                        break;
                    case NUMBER:
                        assertion.assertions.put(key, reader.getInt());
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return null;
        });

        return assertion;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        writeContent(jsonWriter);
        return jsonWriter.writeEndObject();
    }

    public void writeContent(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeFieldName("assert");
        jsonWriter.writeStartObject();

        for (Map.Entry<String, Object> entry : assertions.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                jsonWriter.writeStringField(key, (String) value);
            } else if (value instanceof Integer) {
                jsonWriter.writeIntField(key, (Integer) value);
            } else if (value instanceof Boolean) {
                jsonWriter.writeBooleanField(key, (Boolean) value);
            }
        }

        jsonWriter.writeEndObject();
    }

    public Map<String, Object> getAssertions() {
        return assertions;
    }

    public Object getAssertion(String key) {
        return assertions.get(key);
    }

    public void addAssertion(String key, Object value) {
        if (value instanceof String || value instanceof Integer || value instanceof Boolean) {
            assertions.put(key, value);
        } else {
            throw new IllegalArgumentException("Assertion value must be String or Integer");
        }
    }
}