// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter
class Credential {

    @SerializedName("home_account_id")
    protected String homeAccountId;

    @SerializedName("environment")
    protected String environment;

    @SerializedName("client_id")
    protected String clientId;

    @SerializedName("secret")
    protected String secret;
}
