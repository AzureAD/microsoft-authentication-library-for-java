// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.google.gson.annotations.SerializedName;

public final class DeviceCode {

    /**
     *  The user code.
     */
    @SerializedName("user_code")
    private String userCode;

    /**
     * The device code.
     */
    @SerializedName("device_code")
    private String deviceCode;

    /**
     * The verification url.
     */
    @SerializedName("verification_url")
    private String verificationUrl;

    /**
     * The expiration time in seconds.
     */
    @SerializedName("expires_in")
    private long expiresIn;

    /**
     * The interval
     */
    @SerializedName("interval")
    private long interval;

    /**
     * The message which should be displayed to the user.
     */
    @SerializedName("message")
    private String message;

    private transient  String correlationId = null;

    private transient  String clientId = null;

    private transient  String scopes = null;

    /**
     * Returns the user code.
     *
     * @return The user code.
     */
    public String getUserCode() {
        return userCode;
    }

    /**
     * Returns the device code.
     *
     * @return The device code.
     */
    public String getDeviceCode() {
        return deviceCode;
    }

    /**
     * Returns the verification URL.
     *
     * @return The verification URL.
     */
    public String getVerificationUrl() {
        return verificationUrl;
    }

    /**
     * Returns the expiration in seconds.
     *
     * @return The expiration time in seconds.
     */
    public long getExpiresIn() {
        return expiresIn;
    }

    /**
     * Returns the interval.
     *
     * @return The interval.
     */
    public long getInterval() {
        return interval;
    }

    /**
     * Returns the message which should be displayed to the user.
     *
     * @return The message.
     */
    public String getMessage() {
        return message;
    }

    protected String getCorrelationId() {
        return correlationId;
    }

    protected void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    protected String getClientId() {
        return clientId;
    }

    protected void setClientId(String clientId) {
        this.clientId = clientId;
    }

    protected String getScopes() {
        return scopes;
    }

    protected void setScopes(String scopes) {
        this.scopes = scopes;
    }
}
