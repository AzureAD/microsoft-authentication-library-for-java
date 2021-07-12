// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Response returned from the STS device code endpoint containing information necessary for
 * device code flow
 */
@Accessors(fluent = true)
@Getter
public final class DeviceCode {

    /**
     * code which user needs to provide when authenticating at the verification URI
     */
    @JsonProperty("user_code")
    private String userCode;

    /**
     * code which should be included in the request for the access token
     */
    @JsonProperty("device_code")
    private String deviceCode;

    /**
     * URI where user can authenticate
     */
    @JsonProperty("verification_uri")
    private String verificationUri;

    /**
     * expiration time of device code in seconds.
     */
    @JsonProperty("expires_in")
    private long expiresIn;

    /**
     * interval at which the STS should be polled at
     */
    @JsonProperty("interval")
    private long interval;

    /**
     * message which should be displayed to the user.
     */
    @JsonProperty("message")
    private String message;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private transient String correlationId = null;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private transient String clientId = null;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private transient String scopes = null;
}
