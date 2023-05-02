// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.ArrayList;
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
}
