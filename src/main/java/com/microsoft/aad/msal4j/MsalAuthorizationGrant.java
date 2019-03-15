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

import java.util.Map;

/**
 * Abstract class for an MSAL grant.
 */
abstract class MsalAuthorizationGrant {

    /**
     *  Converts the grant into a HTTP parameters map.
     *
     * @return A map contains the HTTP parameters
     */
    abstract Map<String, String> toParameters();

    static final String SCOPE_PARAM_NAME = "scope";
    static final String SCOPES_DELIMITER = " ";

    static final String SCOPE_OPEN_ID = "openid";
    static final String SCOPE_PROFILE = "profile";
    static final String SCOPE_OFFLINE_ACCESS = "offline_access";

    static final String COMMON_SCOPES_PARAM = SCOPE_OPEN_ID + SCOPES_DELIMITER +
            SCOPE_PROFILE + SCOPES_DELIMITER +
            SCOPE_OFFLINE_ACCESS;
}
