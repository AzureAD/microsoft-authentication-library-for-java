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
class AccessTokenCacheEntity extends Credential {

    @JsonProperty("credential_type")
    private String credentialType;

    @JsonProperty("realm")
    protected String realm;

    @JsonProperty("target")
    private String target;

    @JsonProperty("cached_at")
    private String cachedAt;

    @JsonProperty("expires_on")
    private String expiresOn;

    @JsonProperty("extended_expires_on")
    private String extExpiresOn;

    @JsonProperty("refresh_on")
    private String refreshOn;

    String getKey() {
        List<String> keyParts = new ArrayList<>();

        keyParts.add(StringHelper.isBlank(homeAccountId) ? "" : homeAccountId);
        keyParts.add(environment);
        keyParts.add(credentialType);
        keyParts.add(clientId);
        keyParts.add(realm);
        keyParts.add(target);

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    public static AccessTokenCacheEntity convertJsonToObject(String json, JsonParser jsonParser) throws IOException {

        if (json != null) {
            AccessTokenCacheEntity accessTokenCacheEntity = new AccessTokenCacheEntity();

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("credential_type".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.credentialType(jsonParser.getText());
                }

                else if ("realm".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.realm(jsonParser.getText());
                }

                else if ("target".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.target(jsonParser.getText());
                }

                else if ("cached_at".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.cachedAt(jsonParser.getText());
                }

                else if ("expires_on".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.expiresOn(jsonParser.getText());
                }

                else if ("extended_expires_on".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.extExpiresOn(jsonParser.getText());
                }

                else if ("refresh_on".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.refreshOn(jsonParser.getText());
                }

                else if ("environment".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.environment(jsonParser.getText());
                }

                else if ("home_account_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.homeAccountId(jsonParser.getText());
                }

                else if ("client_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.clientId(jsonParser.getText());
                }

                else if ("secret".equals(fieldname)) {
                    jsonParser.nextToken();
                    accessTokenCacheEntity.secret(jsonParser.getText());
                }
            }
            return accessTokenCacheEntity;
        }
        return new AccessTokenCacheEntity();
    }

    public JSONObject convertToJSONObject(){
        JSONObject jsonObject = new JSONObject();
        List<String> fieldSet =
                Arrays.asList("credential_type", "realm", "target", "cached_at", "expires_on",
                        "extended_expires_on", "refresh_on", "environment", "home_account_id",
                        "client_id", "secret");
        jsonObject.put(fieldSet.get(0), this.credentialType);
        jsonObject.put(fieldSet.get(1), this.realm);
        jsonObject.put(fieldSet.get(2), this.target);
        jsonObject.put(fieldSet.get(3), this.cachedAt);
        jsonObject.put(fieldSet.get(4), this.expiresOn);
        jsonObject.put(fieldSet.get(5), this.extExpiresOn);
        jsonObject.put(fieldSet.get(6), this.refreshOn);
        jsonObject.put(fieldSet.get(7), this.environment);
        jsonObject.put(fieldSet.get(8), this.homeAccountId);
        jsonObject.put(fieldSet.get(9), this.clientId);
        jsonObject.put(fieldSet.get(10), this.secret);
        return jsonObject;
    }
}
