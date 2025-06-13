// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Steps implements JsonSerializable<Steps> {
    private final List<Step> steps = new ArrayList<>();

    public static Steps fromJson(JsonReader jsonReader) throws IOException {
        Steps steps = new Steps();

        if (jsonReader.currentToken() == JsonToken.START_OBJECT) {
            jsonReader.readObject(reader -> {
                while (reader.nextToken() != JsonToken.END_OBJECT) {
                    String field = reader.getFieldName();
                    if ("steps".equals(field)) {
                        reader.nextToken(); // Move to START_ARRAY
                        parseStepsArray(reader, steps);
                    } else {
                        reader.nextToken();
                        reader.skipChildren();
                    }
                }
                return null;
            });
        } else if (jsonReader.currentToken() == JsonToken.START_ARRAY) {
            parseStepsArray(jsonReader, steps);
        }

        return steps;
    }

    private static void parseStepsArray(JsonReader reader, Steps steps) throws IOException {
        while (reader.nextToken() != JsonToken.END_ARRAY) {
            if (reader.currentToken() == JsonToken.START_OBJECT) {
                steps.addStep(Step.fromJson(reader));
            }
        }
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        writeContent(jsonWriter);
        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    public void writeContent(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeFieldName("steps");
        jsonWriter.writeStartArray();
        for (Step step : steps) {
            step.toJson(jsonWriter);
        }
        jsonWriter.writeEndArray();
    }

    public List<Step> getSteps() {
        return steps;
    }

    public void addStep(Step step) {
        steps.add(step);
    }

    public static class Step implements JsonSerializable<Step> {
        private Act action;
        private Assert assertion;

        public static Step fromJson(JsonReader jsonReader) throws IOException {
            Step step = new Step();

            jsonReader.readObject(reader -> {
                while (reader.nextToken() != JsonToken.END_OBJECT) {
                    String field = reader.getFieldName();
                    reader.nextToken();

                    switch (field) {
                        case "act":
                            step.action = Act.fromJson(reader);
                            break;
                        case "assert":
                            step.assertion = Assert.fromJson(reader);
                            break;
                        default:
                            reader.skipChildren();
                    }
                }
                return null;
            });

            return step;
        }

        @Override
        public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
            jsonWriter.writeStartObject();

            if (action != null) {
                action.writeContent(jsonWriter);
            }

            if (assertion != null) {
                assertion.writeContent(jsonWriter);
            }

            return jsonWriter.writeEndObject();
        }

        public Act getAction() {
            return action;
        }

        public void setAction(Act action) {
            this.action = action;
        }

        public Assert getAssertion() {
            return assertion;
        }

        public void setAssertion(Assert assertion) {
            this.assertion = assertion;
        }
    }
}