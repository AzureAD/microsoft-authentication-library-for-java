// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;
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
    @SerializedName("user_code")
    private String userCode;

    /**
     * code which should be included in the request for the access token
     */
    @SerializedName("device_code")
    private String deviceCode;

    /**
     * URI where user can authenticate
     */
    @SerializedName("verification_uri")
    private String verificationUri;

    /**
     * expiration time of device code in seconds.
     */
    @SerializedName("expires_in")
    private long expiresIn;

    /**
     * interval at which the STS should be polled at
     */
    @SerializedName("interval")
    private long interval;

    /**
     * message which should be displayed to the user.
     */
    @SerializedName("message")
    private String message;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private transient  String correlationId = null;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private transient  String clientId = null;

    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private transient  String scopes = null;
}
