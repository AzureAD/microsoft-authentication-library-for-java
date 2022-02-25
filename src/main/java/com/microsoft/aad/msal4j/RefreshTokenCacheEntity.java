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
class RefreshTokenCacheEntity extends Credential {

    @JsonProperty("credential_type")
    private String credentialType;

    @JsonProperty("family_id")
    private String family_id;

    boolean isFamilyRT() {
        return !StringHelper.isBlank(family_id);
    }

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);

        if (isFamilyRT()) {
            keyParts.add(family_id);
        } else {
            keyParts.add(clientId);
        }

        // realm
        keyParts.add("");
        // target
        keyParts.add("");

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    public static RefreshTokenCacheEntity convertJsonToObject(String json, JsonParser jsonParser) throws IOException {

        RefreshTokenCacheEntity refreshTokenCacheEntity = new RefreshTokenCacheEntity();

        if (json != null) {

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("credential_type".equals(fieldname)) {
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.credentialType = jsonParser.getText();
                }

                else if ("family_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.family_id = jsonParser.getText();
                }

                else if ("home_account_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.homeAccountId = jsonParser.getText();
                }

                else if ("environment".equals(fieldname)) {
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.environment = jsonParser.getText();
                }

                else if ("client_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.clientId = jsonParser.getText();
                }

                else if ("secret".equals(fieldname)) {
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.secret = jsonParser.getText();
                }

                else if("user_assertion_hash".equals(fieldname)){
                    jsonParser.nextToken();
                    refreshTokenCacheEntity.userAssertionHash = jsonParser.getText();
                }
            }
        }
        return refreshTokenCacheEntity;
    }

    public JSONObject convertToJSONObject(){
        JSONObject jsonObject = new JSONObject();
        List<String> fieldSet =
                Arrays.asList("credential_type", "family_id", "home_account_id",
                        "environment", "client_id", "secret",
                        "user_assertion_hash"
                );
        jsonObject.put(fieldSet.get(0), this.credentialType);
        jsonObject.put(fieldSet.get(1), this.family_id);
        jsonObject.put(fieldSet.get(2), this.homeAccountId);
        jsonObject.put(fieldSet.get(3), this.environment);
        jsonObject.put(fieldSet.get(4), this.clientId);
        jsonObject.put(fieldSet.get(5), this.secret);
        jsonObject.put(fieldSet.get(6), this.userAssertionHash);
        return jsonObject;
    }
}
