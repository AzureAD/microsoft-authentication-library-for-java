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

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import net.jcip.annotations.Immutable;

/**
 * Error object used to encapsulated the device code errors.
 */
@Immutable
public class DeviceCodeTokenErrorResponse extends TokenErrorResponse {
    /**
     * Creates a new device code token error response.
     *
     * @param error The error.
     */
    public DeviceCodeTokenErrorResponse(final ErrorObject error) {
        super(error);
    }

    /**
     * Checks if is a device code error.
     *
     * @return true if is one of the well known device code error code, otherwise false.
     */
    public boolean isDeviceCodeError() {
        ErrorObject errorObject = getErrorObject();
        if (errorObject == null) {
            return false;
        }
        String code = errorObject.getCode();
        if (code == null) {
            return false;
        }
        switch (code) {
            case "authorization_pending":
            case "slow_down":
            case "access_denied":
            case "code_expired":
                return true;
            default:
                return false;
        }
    }

    /**
     * Parses an device code Token Error response from the specified HTTP
     * response.
     *
     * @param httpResponse The HTTP response to parse.
     *
     * @return A DeviceCodeTokenErrorResponse which may contain a device code error.
     */
    public static DeviceCodeTokenErrorResponse parse(final HTTPResponse httpResponse) {
        return new DeviceCodeTokenErrorResponse(ErrorObject.parse(httpResponse));
    }

}
