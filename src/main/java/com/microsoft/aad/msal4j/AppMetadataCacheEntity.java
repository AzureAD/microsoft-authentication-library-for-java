// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
class AppMetadataCacheEntity {

    public static final String APP_METADATA_CACHE_ENTITY_ID = "appmetadata";

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("environment")
    private String environment;

    @JsonProperty("family_id")
    private String familyId;

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(APP_METADATA_CACHE_ENTITY_ID);
        keyParts.add(environment);
        keyParts.add(clientId);

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    public static AppMetadataCacheEntity convertJsonToObject(String json, JsonParser jsonParser) throws IOException {
        AppMetadataCacheEntity appMetadataCacheEntity = new AppMetadataCacheEntity();

        if (json != null) {

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("client_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    appMetadataCacheEntity.clientId = jsonParser.getText();
                }

                else if ("environment".equals(fieldname)) {
                    jsonParser.nextToken();
                    appMetadataCacheEntity.environment = jsonParser.getText();
                }

                else if ("family_id".equals(fieldname)) {

                    jsonParser.nextToken();
                    if(jsonParser.currentToken()!=JsonToken.VALUE_NULL) {
                        appMetadataCacheEntity.familyId = jsonParser.getText();
                    }
                }

            }
        }
        return appMetadataCacheEntity;
    }

    public JSONObject convertToJSONObject(){
        JSONObject jsonObject = new JSONObject();
        List<String> fieldSet =
                Arrays.asList("client_id", "environment", "family_id");
        jsonObject.put(fieldSet.get(0), this.clientId);
        jsonObject.put(fieldSet.get(1), this.environment);
        jsonObject.put(fieldSet.get(2), this.familyId);
        return jsonObject;
    }
}
