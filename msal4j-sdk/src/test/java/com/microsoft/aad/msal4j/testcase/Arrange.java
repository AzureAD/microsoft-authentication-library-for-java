// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Arrange implements JsonSerializable<Arrange> {
    private final Map<String, ObjectProperties> objects = new LinkedHashMap<>();

    public static Arrange fromJson(JsonReader jsonReader) throws IOException {
        Arrange arrange = new Arrange();

        jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String objectName = reader.getFieldName();
                System.out.println("Reading object: " + objectName);
                reader.nextToken();
                arrange.objects.put(objectName, ObjectProperties.fromJson(reader));
            }
            return null;
        });

        return arrange;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        writeContent(jsonWriter);
        return jsonWriter.writeEndObject();
    }

    public void writeContent(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeFieldName("arrange");
        jsonWriter.writeStartObject();

        for (Map.Entry<String, ObjectProperties> entry : objects.entrySet()) {
            jsonWriter.writeFieldName(entry.getKey());
            entry.getValue().toJson(jsonWriter);
        }

        jsonWriter.writeEndObject();
    }

    public ObjectProperties getObject(String name) {
        return objects.get(name);
    }

    public Map<String, ObjectProperties> getObjects() {
        return objects;
    }

    public void addObject(String name, ObjectProperties objectProperties) {
        objects.put(name, objectProperties);
    }

    public static class ObjectProperties implements JsonSerializable<ObjectProperties> {
        private String type;
        private final Map<String, java.lang.Object> properties = new HashMap<>();

        public static ObjectProperties fromJson(JsonReader jsonReader) throws IOException {
            ObjectProperties objectProperties = new ObjectProperties();

            jsonReader.readObject(reader -> {
                while (reader.nextToken() != JsonToken.END_OBJECT) {
                    String className = reader.getFieldName();
                    objectProperties.type = className;
                    reader.nextToken();

                    reader.readObject(classReader -> {
                        while (classReader.nextToken() != JsonToken.END_OBJECT) {
                            String propertyGroup = classReader.getFieldName();
                            classReader.nextToken();

                            switch (classReader.currentToken()) {
                                case START_ARRAY:
                                    objectProperties.properties.put(propertyGroup,
                                            classReader.readArray(JsonReader::getString));
                                    break;
                                case START_OBJECT:
                                    objectProperties.properties.put(propertyGroup,
                                            parseNestedObject(classReader));
                                    break;
                                case STRING:
                                    objectProperties.properties.put(propertyGroup, classReader.getString());
                                    break;
                                case NUMBER:
                                    objectProperties.properties.put(propertyGroup, classReader.getInt());
                                    break;
                                case NULL:
                                    objectProperties.properties.put(propertyGroup, null);
                                    break;
                                default:
                                    classReader.skipChildren();
                                    break;
                            }
                        }
                        return null;
                    });
                }
                return null;
            });

            return objectProperties;
        }

        private static Map<String, java.lang.Object> parseNestedObject(JsonReader reader) throws IOException {
            Map<String, java.lang.Object> nestedProps = new HashMap<>();

            reader.readObject(propsReader -> {
                while (propsReader.nextToken() != JsonToken.END_OBJECT) {
                    String propName = propsReader.getFieldName();
                    propsReader.nextToken();

                    switch (propsReader.currentToken()) {
                        case STRING:
                            nestedProps.put(propName, propsReader.getString());
                            break;
                        case NUMBER:
                            nestedProps.put(propName, propsReader.getInt());
                            break;
                        default:
                            propsReader.skipChildren();
                            break;
                    }
                }
                return null;
            });

            return nestedProps;
        }

        @Override
        public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
            jsonWriter.writeStartObject();
            writeContent(jsonWriter);
            return jsonWriter.writeEndObject();
        }

        public void writeContent(JsonWriter jsonWriter) throws IOException {
            jsonWriter.writeFieldName(type);
            jsonWriter.writeStartObject();

            for (Map.Entry<String, java.lang.Object> entry : properties.entrySet()) {
                jsonWriter.writeFieldName(entry.getKey());
                java.lang.Object value = entry.getValue();

                if (value instanceof List) {
                    writeArray(jsonWriter, (List<String>) value);
                } else if (value instanceof Map) {
                    writeNestedObject(jsonWriter, (Map<String, java.lang.Object>) value);
                } else if (value instanceof String) {
                    jsonWriter.writeString((String) value);
                } else if (value instanceof Integer) {
                    jsonWriter.writeNumber((Integer) value);
                } else if (value == null) {
                    jsonWriter.writeNull();
                }
            }

            jsonWriter.writeEndObject();
        }

        private void writeArray(JsonWriter jsonWriter, List<String> values) throws IOException {
            jsonWriter.writeStartArray();
            for (String value : values) {
                jsonWriter.writeString(value);
            }
            jsonWriter.writeEndArray();
        }

        private void writeNestedObject(JsonWriter jsonWriter, Map<String, java.lang.Object> nestedProps) throws IOException {
            jsonWriter.writeStartObject();

            for (Map.Entry<String, java.lang.Object> prop : nestedProps.entrySet()) {
                String key = prop.getKey();
                java.lang.Object value = prop.getValue();

                if (value instanceof String) {
                    jsonWriter.writeStringField(key, (String) value);
                } else if (value instanceof Integer) {
                    jsonWriter.writeIntField(key, (Integer) value);
                }
            }

            jsonWriter.writeEndObject();
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public java.lang.Object getProperty(String name) {
            return properties.get(name);
        }

        public Map<String, java.lang.Object> getProperties() {
            return properties;
        }

        public void addProperty(String name, java.lang.Object value) {
            if (value instanceof String || value instanceof List ||
                    value instanceof Integer || value instanceof Boolean ||
                    value instanceof Map) {
                properties.put(name, value);
            } else {
                throw new IllegalArgumentException("Unsupported property type: " +
                        (value != null ? value.getClass().getName() : "null"));
            }
        }
    }
}