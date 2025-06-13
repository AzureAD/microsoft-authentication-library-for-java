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

public class Env implements JsonSerializable<Env> {
    private final Map<String, Object> variables = new HashMap<>();

    public static Env fromJson(JsonReader jsonReader) throws IOException {
        Env env = new Env();

        jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String key = reader.getFieldName();
                reader.nextToken(); // Move to the value

                switch (reader.currentToken()) {
                    case STRING:
                        env.variables.put(key, reader.getString());
                        break;
                    case NUMBER:
                        env.variables.put(key, reader.getInt());
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return null;
        });

        return env;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        writeContent(jsonWriter);
        return jsonWriter.writeEndObject();
    }

    public void writeContent(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeFieldName("env");
        jsonWriter.writeStartObject();

        for (Map.Entry<String, Object> entry : variables.entrySet()) {
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

    public Map<String, Object> getVariables() {
        return variables;
    }

    public Object getVariable(String name) {
        return variables.get(name);
    }

    public void setVariable(String name, Object value) {
        if (value instanceof String || value instanceof Integer || value instanceof Boolean) {
            variables.put(name, value);
        } else {
            throw new IllegalArgumentException("Variable value must be String, Integer, or Boolean");
        }
    }
}