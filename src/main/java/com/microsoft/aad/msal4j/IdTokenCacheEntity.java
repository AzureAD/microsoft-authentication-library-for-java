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
class IdTokenCacheEntity extends Credential {

    @JsonProperty("credential_type")
    private String credentialType;

    @JsonProperty("realm")
    protected String realm;

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);
        keyParts.add(clientId);
        keyParts.add(realm);

        // target
        keyParts.add("");

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    public static IdTokenCacheEntity convertJsonToObject(String json, JsonParser jsonParser) throws IOException {
        IdTokenCacheEntity idTokenCacheEntity = new IdTokenCacheEntity();

        if (json != null) {

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("credential_type".equals(fieldname)) {
                    jsonParser.nextToken();
                    idTokenCacheEntity.credentialType = jsonParser.getText();
                }

                else if ("realm".equals(fieldname)) {
                    jsonParser.nextToken();
                    idTokenCacheEntity.realm = jsonParser.getText();
                }

                else if ("home_account_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    idTokenCacheEntity.homeAccountId = jsonParser.getText();
                }

                else if ("client_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    idTokenCacheEntity.clientId = jsonParser.getText();
                }

                else if ("secret".equals(fieldname)) {
                    jsonParser.nextToken();
                    idTokenCacheEntity.secret = jsonParser.getText();
                }

                else if ("environment".equals(fieldname)) {
                    jsonParser.nextToken();
                    idTokenCacheEntity.environment = jsonParser.getText();
                }

            }
        }
        return idTokenCacheEntity;
    }

    public JSONObject convertToJSONObject(){
        JSONObject jsonObject = new JSONObject();
        List<String> fieldSet =
                Arrays.asList("credential_type", "realm", "home_account_id",
                        "client_id", "secret",  "environment");
            jsonObject.put(fieldSet.get(0), this.credentialType);
            jsonObject.put(fieldSet.get(1), this.realm);
            jsonObject.put(fieldSet.get(2), this.homeAccountId);
            jsonObject.put(fieldSet.get(3), this.clientId);
            jsonObject.put(fieldSet.get(4), this.secret);
            jsonObject.put(fieldSet.get(5), this.environment);
            return jsonObject;


    }
}
