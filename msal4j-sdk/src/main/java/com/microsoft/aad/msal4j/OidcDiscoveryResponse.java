// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class OidcDiscoveryResponse {

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("device_authorization_endpoint")
    private String deviceCodeEndpoint;
}
