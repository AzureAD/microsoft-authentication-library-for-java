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
     * code which user needs to provide when authenticating at he verification URI
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
