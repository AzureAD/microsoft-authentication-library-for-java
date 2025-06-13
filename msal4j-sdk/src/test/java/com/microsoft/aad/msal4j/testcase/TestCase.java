// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.testcase;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

public class TestCase implements JsonSerializable<TestCase> {
    private String type;
    private int version;
    private Arrange arrange;
    private Env env;
    private Steps steps;

    public static TestCase fromJson(JsonReader jsonReader) throws IOException {
        TestCase testCase = new TestCase();

        jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String field = reader.getFieldName();
                reader.nextToken(); // Move to value

                switch (field) {
                    case "type":
                        testCase.type = reader.getString();
                        break;
                    case "ver":
                        testCase.version = reader.getInt();
                        break;
                    case "arrange":
                        testCase.arrange = Arrange.fromJson(reader);
                        break;
                    case "env":
                        testCase.env = Env.fromJson(reader);
                        break;
                    case "steps":
                        testCase.steps = Steps.fromJson(reader);
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return null;
        });

        return testCase;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();
        writeContent(jsonWriter);
        return jsonWriter.writeEndObject();
    }

    public void writeContent(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStringField("type", type);
        jsonWriter.writeIntField("ver", version);

        if (arrange != null) {
            arrange.writeContent(jsonWriter);
        }

        if (env != null) {
            env.writeContent(jsonWriter);
        }

        if (steps != null) {
            steps.writeContent(jsonWriter);
        }
    }

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public Arrange getArrange() {
        return arrange;
    }

    public void setArrange(Arrange arrange) {
        this.arrange = arrange;
    }

    public Env getEnv() {
        return env;
    }

    public void setEnv(Env env) {
        this.env = env;
    }

    public Steps getSteps() {
        return steps;
    }

    public void setSteps(Steps steps) {
        this.steps = steps;
    }
}