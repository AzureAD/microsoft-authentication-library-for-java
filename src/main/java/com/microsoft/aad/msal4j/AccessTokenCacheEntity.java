// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
@Getter
@Setter
class AccessTokenCacheEntity extends Credential {

    @SerializedName("credential_type")
    private String credentialType;

    @SerializedName("realm")
    protected String realm;

    @SerializedName("target")
    private String target;

    @SerializedName("cached_at")
    private String cachedAt;

    @SerializedName("expires_on")
    private String expiresOn;

    @SerializedName("extended_expires_on")
    private String extExpiresOn;

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
}
