// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the additional information that can be sent to an authorization server for a request claim in the claim request parameter
 *
 * @see <a href="https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter">https://openid.net/specs/openid-connect-core-1_0-final.html#ClaimsParameter</a>
 */
public class RequestedClaimAdditionalInfo implements JsonSerializable<RequestedClaimAdditionalInfo> {

    private boolean essential;
    private String value;
    private List<String> values;

    public RequestedClaimAdditionalInfo(boolean essential, String value, List<String> values) {
        this.essential = essential;
        this.value = value;
        this.values = values;
    }

    public RequestedClaimAdditionalInfo fromJson(JsonReader jsonReader) throws IOException {
        if (jsonReader.currentToken() != JsonToken.START_OBJECT) {
            jsonReader.nextToken();
            if (jsonReader.currentToken() != JsonToken.START_OBJECT) {
                throw new IllegalStateException("Expected start of object but was " + jsonReader.currentToken());
            }
        }

        while (jsonReader.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = jsonReader.getFieldName();
            jsonReader.nextToken();

            switch (fieldName) {
                case "essential":
                    essential = jsonReader.getBoolean();
                    break;
                case "value":
                    value = jsonReader.getString();
                    break;
                case "values":
                    values = new ArrayList<>();
                    while (jsonReader.nextToken() != JsonToken.END_ARRAY) {
                        values.add(jsonReader.getString());
                    }
                    break;
                default:
                    jsonReader.skipChildren();
                    break;
            }
        }

        return this;
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        if (essential) {
            jsonWriter.writeBooleanField("essential", essential);
        }

        if (value != null) {
            jsonWriter.writeStringField("value", value);
        }

        if (values != null && !values.isEmpty()) {
            jsonWriter.writeStartArray("values");
            for (String val : values) {
                jsonWriter.writeString(val);
            }
            jsonWriter.writeEndArray();
        }

        jsonWriter.writeEndObject();
        return jsonWriter;
    }

    public boolean isEssential() {
        return this.essential;
    }

    public String getValue() {
        return this.value;
    }

    public List<String> getValues() {
        return this.values;
    }

    public void setEssential(boolean essential) {
        this.essential = essential;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }
}