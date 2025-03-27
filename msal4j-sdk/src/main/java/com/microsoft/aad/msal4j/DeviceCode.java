// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response returned from the STS device code endpoint containing information necessary for
 * device code flow
 */
public final class DeviceCode {

    @JsonProperty("user_code")
    private String userCode;

    @JsonProperty("device_code")
    private String deviceCode;

    @JsonProperty("verification_uri")
    private String verificationUri;

    @JsonProperty("expires_in")
    private long expiresIn;

    @JsonProperty("interval")
    private long interval;

    @JsonProperty("message")
    private String message;

    private transient String correlationId = null;
    private transient String clientId = null;
    private transient String scopes = null;

    /**
     * code which user needs to provide when authenticating at the verification URI
     */
    public String userCode() {
        return this.userCode;
    }

    /**
     * code which should be included in the request for the access token
     */
    public String deviceCode() {
        return this.deviceCode;
    }

    /**
     * URI where user can authenticate
     */
    public String verificationUri() {
        return this.verificationUri;
    }

    /**
     * expiration time of device code in seconds.
     */
    public long expiresIn() {
        return this.expiresIn;
    }

    /**
     * interval at which the STS should be polled at
     */
    public long interval() {
        return this.interval;
    }

    /**
     * message which should be displayed to the user.
     */
    public String message() {
        return this.message;
    }

    protected String correlationId() {
        return this.correlationId;
    }

    protected String clientId() {
        return this.clientId;
    }

    protected String scopes() {
        return this.scopes;
    }

    protected DeviceCode correlationId(String correlationId) {
        this.correlationId = correlationId;
        return this;
    }

    protected DeviceCode clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    protected DeviceCode scopes(String scopes) {
        this.scopes = scopes;
        return this;
    }
}
