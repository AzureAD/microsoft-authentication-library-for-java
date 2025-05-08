// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;

public class UserSecret implements JsonSerializable<UserSecret> {

    String secret;
    String value;

    static UserSecret fromJson(JsonReader jsonReader) throws IOException {
        UserSecret userSecret = new UserSecret();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "secret":
                        userSecret.secret = reader.getString();
                        break;
                    case "value":
                        userSecret.value = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return userSecret;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("secret", secret);
        jsonWriter.writeStringField("value", value);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }
}