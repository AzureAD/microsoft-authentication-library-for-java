// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;

import java.io.IOException;

@Getter
public class Lab {
    @JsonProperty("labName")
    String labName;

    @JsonProperty("domain")
    String domain;

    @JsonProperty("tenantId")
    String tenantId;

    @JsonProperty("federationProvider")
    String federationProvider;

    @JsonProperty("azureEnvironment")
    String azureEnvironment;

    @JsonProperty("authority")
    String authority;

    public static Lab convertJsonToObject(String json) throws IOException {

        if (json != null) {
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createParser(json);

            if (jsonParser.nextToken().equals(JsonToken.START_ARRAY)) {
                jsonParser.nextToken();
            }

            Lab lab = new Lab();

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("labName".equals(fieldname)) {
                    jsonParser.nextToken();
                    lab.labName = jsonParser.getText();
                }

                else if ("domain".equals(fieldname)) {
                    jsonParser.nextToken();
                    lab.domain = jsonParser.getText();
                }

                else if ("tenantId".equals(fieldname)) {
                    jsonParser.nextToken();
                    lab.tenantId = jsonParser.getText();
                }

                else if ("federationProvider".equals(fieldname)) {
                    jsonParser.nextToken();
                    lab.federationProvider = jsonParser.getText();
                }

                else if ("azureEnvironment".equals(fieldname)) {
                    jsonParser.nextToken();
                    lab.azureEnvironment = jsonParser.getText();
                }

                else if ("authority".equals(fieldname)) {
                    jsonParser.nextToken();
                    lab.authority = jsonParser.getText();
                }

            }
            return lab;
        }
        return new Lab();
    }
}
