// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Act implements JsonSerializable<Act> {
    private String targetObject;
    private String methodName;
    private final Map<String, Object> parameters = new HashMap<>();

    public static Act fromJson(JsonReader jsonReader) throws IOException {
        Act action = new Act();

        jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String key = reader.getFieldName();
                reader.nextToken();

                // Split "objectName.methodName"
                int dotIndex = key.indexOf('.');
                if (dotIndex == -1) {
                    throw new IOException("Invalid act key: " + key);
                }
                action.targetObject = key.substring(0, dotIndex);
                action.methodName = key.substring(dotIndex + 1);

                // Read parameters object
                reader.readObject(paramReader -> {
                    while (paramReader.nextToken() != JsonToken.END_OBJECT) {
                        String paramName = paramReader.getFieldName();
                        paramReader.nextToken();
                        switch (paramReader.currentToken()) {
                            case STRING:
                                action.parameters.put(paramName, paramReader.getString());
                                break;
                            case NUMBER:
                                action.parameters.put(paramName, paramReader.getInt());
                                break;
                            case START_ARRAY:
                                if ("scopes".equals(paramName)) {
                                    action.parameters.put(paramName, paramReader.readArray(JsonReader::getString));
                                } else {
                                    paramReader.skipChildren();
                                }
                                break;
                            default:
                                paramReader.skipChildren();
                                break;
                        }
                    }
                    return null;
                });
            }
            return null;
        });

        return action;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        writeContent(jsonWriter);
        return jsonWriter.writeEndObject();
    }

    public void writeContent(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeFieldName("act");
        jsonWriter.writeStartObject();
        jsonWriter.writeFieldName(targetObject + "." + methodName);
        jsonWriter.writeStartObject();

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String) {
                jsonWriter.writeStringField(key, (String) value);
            } else if (value instanceof Integer) {
                jsonWriter.writeIntField(key, (Integer) value);
            } else if (value instanceof List) {
                jsonWriter.writeFieldName(key);
                jsonWriter.writeStartArray();
                for (String val : (List<String>) value) {
                    jsonWriter.writeString(val);
                }
                jsonWriter.writeEndArray();
            }
        }

        jsonWriter.writeEndObject();
        jsonWriter.writeEndObject();
    }

    public String getTargetObject() {
        return targetObject;
    }

    public String getMethodName() {
        return methodName;
    }

    public Object getParameter(String name) {
        return parameters.get(name);
    }

    public boolean hasParameter(String name) {
        return parameters.containsKey(name);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }
}