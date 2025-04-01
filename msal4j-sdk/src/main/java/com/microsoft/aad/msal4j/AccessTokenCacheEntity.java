// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

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

    String credentialType() {
        return this.credentialType;
    }

    String realm() {
        return this.realm;
    }

    String target() {
        return this.target;
    }

    String cachedAt() {
        return this.cachedAt;
    }

    String expiresOn() {
        return this.expiresOn;
    }

    String extExpiresOn() {
        return this.extExpiresOn;
    }

    String refreshOn() {
        return this.refreshOn;
    }

    void credentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    void realm(String realm) {
        this.realm = realm;
    }

    void target(String target) {
        this.target = target;
    }

    void cachedAt(String cachedAt) {
        this.cachedAt = cachedAt;
    }

    void expiresOn(String expiresOn) {
        this.expiresOn = expiresOn;
    }

    void extExpiresOn(String extExpiresOn) {
        this.extExpiresOn = extExpiresOn;
    }

    void refreshOn(String refreshOn) {
        this.refreshOn = refreshOn;
    }
}
