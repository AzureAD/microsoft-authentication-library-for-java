// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.*;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
@EqualsAndHashCode
class AccountCacheEntity implements Serializable {

    static final String MSSTS_ACCOUNT_TYPE = "MSSTS";
    static final String ADFS_ACCOUNT_TYPE = "ADFS";

    @SerializedName("home_account_id")
    protected String homeAccountId;

    @SerializedName("environment")
    protected String environment;

    @EqualsAndHashCode.Exclude
    @SerializedName("realm")
    protected String realm;

    @SerializedName("local_account_id")
    protected String localAccountId;

    @SerializedName("username")
    protected String username;

    @SerializedName("name")
    protected String name;

    @SerializedName("client_info")
    protected String clientInfoStr;

    ClientInfo clientInfo() {
        return ClientInfo.createFromJson(clientInfoStr);
    }

    @SerializedName("authority_type")
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

    static AccountCacheEntity create(String clientInfoStr, Authority requestAuthority, IdToken idToken){
        return create(clientInfoStr, requestAuthority, idToken, null);
    }

    IAccount toAccount(){
        return new Account(homeAccountId, environment, username);
    }
}
