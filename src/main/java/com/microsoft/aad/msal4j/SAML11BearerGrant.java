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

import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.oauth2.sdk.GrantType;
import com.nimbusds.oauth2.sdk.SAML2BearerGrant;

class SAML11BearerGrant extends SAML2BearerGrant {

    /**
     * The grant type.
     */
    public static GrantType grantType = new GrantType(
            "urn:ietf:params:oauth:grant-type:saml1_1-bearer");

    public SAML11BearerGrant(Base64URL assertion) {
        super(assertion);
    }

    @Override
    public Map<String, String> toParameters() {

        Map<String, String> params = super.toParameters();
        params.put("grant_type", grantType.getValue());
        return params;
    }
}
