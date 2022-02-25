// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import lombok.*;
import lombok.experimental.Accessors;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode
class AccountCacheEntity implements Serializable {

    static final String MSSTS_ACCOUNT_TYPE = "MSSTS";
    static final String ADFS_ACCOUNT_TYPE = "ADFS";

    @JsonProperty("home_account_id")
    protected String homeAccountId;

    @JsonProperty("environment")
    protected String environment;

    @JsonProperty("realm")
    protected String realm;

    @JsonProperty("local_account_id")
    protected String localAccountId;

    @JsonProperty("username")
    protected String username;

    @JsonProperty("name")
    protected String name;

    @JsonProperty("client_info")
    protected String clientInfoStr;

    @JsonProperty("user_assertion_hash")
    protected String userAssertionHash;

    ClientInfo clientInfo() {
        return ClientInfo.createFromJson(clientInfoStr);
    }

    @JsonProperty("authority_type")
    protected String authorityType;

    String getKey() {

        List<String> keyParts = new ArrayList<>();

        keyParts.add(homeAccountId);
        keyParts.add(environment);
        keyParts.add(StringHelper.isBlank(realm) ? "" : realm);

        return String.join(Constants.CACHE_KEY_SEPARATOR, keyParts).toLowerCase();
    }

    static AccountCacheEntity create(String clientInfoStr, Authority requestAuthority, IdToken idToken, String policy) {

        AccountCacheEntity account = new AccountCacheEntity();
        account.authorityType(MSSTS_ACCOUNT_TYPE);
        account.clientInfoStr = clientInfoStr;
        account.homeAccountId(policy != null ?
                account.clientInfo().toAccountIdentifier() + Constants.CACHE_KEY_SEPARATOR + policy :
                account.clientInfo().toAccountIdentifier());
        account.environment(requestAuthority.host());
        account.realm(requestAuthority.tenant());

        if (idToken != null) {
            String localAccountId = !StringHelper.isBlank(idToken.objectIdentifier)
                    ? idToken.objectIdentifier : idToken.subject;
            account.localAccountId(localAccountId);
            account.username(idToken.preferredUsername);
            account.name(idToken.name);
        }

        return account;
    }

    static AccountCacheEntity createADFSAccount(Authority requestAuthority, IdToken idToken) {

        AccountCacheEntity account = new AccountCacheEntity();
        account.authorityType(ADFS_ACCOUNT_TYPE);
        account.homeAccountId(idToken.subject);

        account.environment(requestAuthority.host());

        account.username(idToken.upn);
        account.name(idToken.uniqueName);

        return account;
    }

    static AccountCacheEntity create(String clientInfoStr, Authority requestAuthority, IdToken idToken) {
        return create(clientInfoStr, requestAuthority, idToken, null);
    }

    IAccount toAccount() {
        return new Account(homeAccountId, environment, username, null);
    }

    public static AccountCacheEntity convertJsonToObject(String json, JsonParser jsonParser) throws IOException {

        AccountCacheEntity accountCacheEntity = new AccountCacheEntity();

        if (json != null) {
            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                String fieldname = jsonParser.getCurrentName();
                if ("home_account_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.homeAccountId = jsonParser.getText();
                }

                else if ("environment".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.environment = jsonParser.getText();
                }

                else if ("realm".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.realm = jsonParser.getText();
                }

                else if ("local_account_id".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.localAccountId = jsonParser.getText();
                }

                else if ("username".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.username = jsonParser.getText();
                }

                else if ("name".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.name = jsonParser.getText();
                }

                else if ("client_info".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.clientInfoStr = jsonParser.getText();
                }

                else if ("user_assertion_hash".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.userAssertionHash = jsonParser.getText();
                }

                else if ("authority_type".equals(fieldname)) {
                    jsonParser.nextToken();
                    accountCacheEntity.authorityType = jsonParser.getText();
                }
            }
        }
        return accountCacheEntity;
    }

    public JSONObject convertToJSONObject(){
        JSONObject jsonObject = new JSONObject();
        List<String> fieldSet =
                Arrays.asList("home_account_id", "environment", "realm", "local_account_id", "username",
                        "name", "client_info", "user_assertion_hash", "authority_type");
        jsonObject.put(fieldSet.get(0), this.homeAccountId);
        jsonObject.put(fieldSet.get(1), this.environment);
        jsonObject.put(fieldSet.get(2), this.realm);
        jsonObject.put(fieldSet.get(3), this.localAccountId);
        jsonObject.put(fieldSet.get(4), this.username);
        jsonObject.put(fieldSet.get(5), this.name);
        jsonObject.put(fieldSet.get(6), this.clientInfoStr);
        jsonObject.put(fieldSet.get(7), this.userAssertionHash);
        jsonObject.put(fieldSet.get(8), this.authorityType);
        return jsonObject;
    }
}
