// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

@Getter
@Setter
public class App {

    @JsonProperty("appType")
    String appType;

    @JsonProperty("appName")
    String appName;

    @JsonProperty("appId")
    String appId;

    @JsonProperty("redirectUri")
    String redirectUri;

    @JsonProperty("authority")
    String authority;

    @JsonProperty("labName")
    String labName;

    @JsonProperty("clientSecret")
    String clientSecret;

    public static App convertJsonToObject(String json) throws IOException {

        if (json != null) {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(json);

            if (jsonParser.nextToken().equals(JsonToken.START_ARRAY)) {
                jsonParser.nextToken();
            }

            App app = new App();

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("appType".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setAppType(jsonParser.getText());
                }

                else if ("appName".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setAppName(jsonParser.getText());
                }


                else if ("appId".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setAppId(jsonParser.getText());
                }

                else if ("redirectUri".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setRedirectUri(jsonParser.getText());
                }

                else if ("authority".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setAuthority(jsonParser.getText());
                }

                else if ("labName".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setLabName(jsonParser.getText());
                }

                else if ("clientSecret".equals(fieldname)) {
                    jsonParser.nextToken();
                    app.setClientSecret(jsonParser.getText());
                }

            }
            return app;
        }

        return new App();
    }
}
