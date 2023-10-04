// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Getter
class ManagedIdentityResponse {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityResponse.class);

    @JsonProperty(value = "token_type")
    String tokenType;

    @JsonProperty(value = "access_token")
    String accessToken;

    @JsonProperty(value = "expires_on")
    String expiresOn;

    String resource;

    @JsonProperty(value = "client_id")
    String clientId;

    /**
     * Creates an access token instance.
     *
     * @param token the token string.
     * @param expiresOn the expiration time.
     */
    @JsonCreator
    private ManagedIdentityResponse(
            @JsonProperty(value = "access_token") String token,
            @JsonProperty(value = "expires_on") String expiresOn) {
        this.accessToken = token;
        this.expiresOn =  expiresOn;
    }
}
