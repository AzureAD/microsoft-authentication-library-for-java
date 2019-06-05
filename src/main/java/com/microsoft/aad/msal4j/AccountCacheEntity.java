// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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

    static AccountCacheEntity create(String clientInfoStr, String environment, IdToken idToken, String policy) {

        AccountCacheEntity account = new AccountCacheEntity();
        account.authorityType(MSSTS_ACCOUNT_TYPE);
        account.clientInfoStr = clientInfoStr;
        account.homeAccountId(policy != null ?
                account.clientInfo().toAccountIdentifier() + Constants.CACHE_KEY_SEPARATOR + policy :
                account.clientInfo().toAccountIdentifier());
        account.environment(environment);

        if (idToken != null) {
            account.realm(idToken.tenantIdentifier);
            String localAccountId = !StringHelper.isBlank(idToken.objectIdentifier)
                    ? idToken.objectIdentifier : idToken.subject;
            account.localAccountId(localAccountId);
            account.username(idToken.preferredUsername);
            account.name(idToken.name);
        }

        return account;
    }

    static AccountCacheEntity create(String clientInfoStr, String environment, IdToken idToken){
        return create(clientInfoStr, environment, idToken, null);
    }

    IAccount toAccount(){
        return new Account(homeAccountId, environment, username);
    }
}
