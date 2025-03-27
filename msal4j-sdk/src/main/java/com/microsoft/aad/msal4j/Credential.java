// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;

class Credential {

    @JsonProperty("home_account_id")
    protected String homeAccountId;

    @JsonProperty("environment")
    protected String environment;

    @JsonProperty("client_id")
    protected String clientId;

    @JsonProperty("secret")
    protected String secret;

    @JsonProperty("user_assertion_hash")
    protected String userAssertionHash;

    String homeAccountId() {
        return this.homeAccountId;
    }

    String environment() {
        return this.environment;
    }

    String clientId() {
        return this.clientId;
    }

    String secret() {
        return this.secret;
    }

    String userAssertionHash() {
        return this.userAssertionHash;
    }

    void homeAccountId(String homeAccountId) {
        this.homeAccountId = homeAccountId;
    }

    void environment(String environment) {
        this.environment = environment;
    }

    void clientId(String clientId) {
        this.clientId = clientId;
    }

    void secret(String secret) {
        this.secret = secret;
    }

    void userAssertionHash(String userAssertionHash) {
        this.userAssertionHash = userAssertionHash;
    }
}
