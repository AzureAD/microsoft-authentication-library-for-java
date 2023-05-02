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
}
