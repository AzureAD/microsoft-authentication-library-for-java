// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonReader;
import com.azure.json.JsonSerializable;
import com.azure.json.JsonToken;
import com.azure.json.JsonWriter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class AccountCacheEntity implements JsonSerializable<AccountCacheEntity>, Serializable {

    static final String MSSTS_ACCOUNT_TYPE = "MSSTS";
    static final String ADFS_ACCOUNT_TYPE = "ADFS";

    protected String homeAccountId;
    protected String environment;
    protected String realm;
    protected String localAccountId;
    protected String username;
    protected String name;
    protected String clientInfoStr;
    protected String userAssertionHash;
    protected String authorityType;

    static AccountCacheEntity fromJson(JsonReader jsonReader) throws IOException {
        AccountCacheEntity entity = new AccountCacheEntity();

        return jsonReader.readObject(reader -> {
            while (reader.nextToken() != JsonToken.END_OBJECT) {
                String fieldName = reader.getFieldName();
                reader.nextToken();

                switch (fieldName) {
                    case "home_account_id":
                        entity.homeAccountId = reader.getString();
                        break;
                    case "environment":
                        entity.environment = reader.getString();
                        break;
                    case "realm":
                        entity.realm = reader.getString();
                        break;
                    case "local_account_id":
                        entity.localAccountId = reader.getString();
                        break;
                    case "username":
                        entity.username = reader.getString();
                        break;
                    case "name":
                        entity.name = reader.getString();
                        break;
                    case "client_info":
                        entity.clientInfoStr = reader.getString();
                        break;
                    case "user_assertion_hash":
                        entity.userAssertionHash = reader.getString();
                        break;
                    case "authority_type":
                        entity.authorityType = reader.getString();
                        break;
                    default:
                        reader.skipChildren();
                        break;
                }
            }
            return entity;
        });
    }

    @Override
    public JsonWriter toJson(JsonWriter jsonWriter) throws IOException {
        jsonWriter.writeStartObject();

        jsonWriter.writeStringField("home_account_id", homeAccountId);
        jsonWriter.writeStringField("environment", environment);
        jsonWriter.writeStringField("realm", realm);
        jsonWriter.writeStringField("local_account_id", localAccountId);
        jsonWriter.writeStringField("username", username);
        jsonWriter.writeStringField("name", name);
        jsonWriter.writeStringField("client_info", clientInfoStr);
        jsonWriter.writeStringField("user_assertion_hash", userAssertionHash);
        jsonWriter.writeStringField("authority_type", authorityType);

        jsonWriter.writeEndObject();

        return jsonWriter;
    }

    ClientInfo clientInfo() {
        return ClientInfo.createFromJson(clientInfoStr);
    }

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

    String homeAccountId() {
        return this.homeAccountId;
    }

    String environment() {
        return this.environment;
    }

    String realm() {
        return this.realm;
    }

    String localAccountId() {
        return this.localAccountId;
    }

    String username() {
        return this.username;
    }

    String name() {
        return this.name;
    }

    String clientInfoStr() {
        return this.clientInfoStr;
    }

    String userAssertionHash() {
        return this.userAssertionHash;
    }

    String authorityType() {
        return this.authorityType;
    }

    void homeAccountId(String homeAccountId) {
        this.homeAccountId = homeAccountId;
    }

    void environment(String environment) {
        this.environment = environment;
    }

    void realm(String realm) {
        this.realm = realm;
    }

    void localAccountId(String localAccountId) {
        this.localAccountId = localAccountId;
    }

    void username(String username) {
        this.username = username;
    }

    void name(String name) {
        this.name = name;
    }

    void clientInfoStr(String clientInfoStr) {
        this.clientInfoStr = clientInfoStr;
    }

    void userAssertionHash(String userAssertionHash) {
        this.userAssertionHash = userAssertionHash;
    }

    void authorityType(String authorityType) {
        this.authorityType = authorityType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AccountCacheEntity)) return false;

        AccountCacheEntity other = (AccountCacheEntity) o;

        if (!Objects.equals(homeAccountId(), other.homeAccountId())) return false;
        if (!Objects.equals(environment(), other.environment())) return false;
        if (!Objects.equals(realm(), other.realm())) return false;
        if (!Objects.equals(localAccountId(), other.localAccountId())) return false;
        if (!Objects.equals(username(), other.username())) return false;
        if (!Objects.equals(name(), other.name())) return false;
        if (!Objects.equals(clientInfoStr(), other.clientInfoStr())) return false;
        if (!Objects.equals(userAssertionHash(), other.userAssertionHash())) return false;
        return Objects.equals(authorityType(), other.authorityType());
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + (this.homeAccountId == null ? 43 : this.homeAccountId.hashCode());
        result = result * 59 + (this.environment == null ? 43 : this.environment.hashCode());
        result = result * 59 + (this.realm == null ? 43 : this.realm.hashCode());
        result = result * 59 + (this.localAccountId == null ? 43 : this.localAccountId.hashCode());
        result = result * 59 + (this.username == null ? 43 : this.username.hashCode());
        result = result * 59 + (this.name() == null ? 43 : this.name().hashCode());
        result = result * 59 + (this.clientInfoStr == null ? 43 : this.clientInfoStr.hashCode());
        result = result * 59 + (this.userAssertionHash == null ? 43 : this.userAssertionHash.hashCode());
        result = result * 59 + (this.authorityType == null ? 43 : this.authorityType.hashCode());
        return result;
    }
}