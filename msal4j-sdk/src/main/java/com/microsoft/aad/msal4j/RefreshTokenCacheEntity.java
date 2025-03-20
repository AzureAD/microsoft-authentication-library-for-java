// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

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

    String credentialType() {
        return this.credentialType;
    }

    String family_id() {
        return this.family_id;
    }

    void credentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    void family_id(String family_id) {
        this.family_id = family_id;
    }
}
