// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;

public class UserSecret {

    @JsonProperty("secret")
    String secret;

    @JsonProperty("value")
    String value;

    public static UserSecret convertJsonToObject(String json) throws IOException {

        if (json != null) {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(json);

            UserSecret userSecret = new UserSecret();

            if(jsonParser.nextToken().equals(JsonToken.START_ARRAY)){
                jsonParser.nextToken();
            }

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("secret".equals(fieldname)) {
                    jsonParser.nextToken();
                    userSecret.secret = jsonParser.getText();
                }

                else if ("value".equals(fieldname)) {
                    jsonParser.nextToken();
                    userSecret.value = jsonParser.getText();
                }

            }

            return userSecret;
        }
        return new UserSecret();
    }
}
