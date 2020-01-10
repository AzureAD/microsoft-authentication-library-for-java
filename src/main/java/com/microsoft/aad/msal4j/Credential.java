// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter
class Credential {

    @JsonProperty("home_account_id")
    protected String homeAccountId;

    @JsonProperty("environment")
    protected String environment;

    @JsonProperty("client_id")
    protected String clientId;

    @JsonProperty("secret")
    protected String secret;
}
