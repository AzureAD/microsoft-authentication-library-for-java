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

import com.nimbusds.oauth2.sdk.AuthorizationGrant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class OAuthAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private AuthorizationGrant grant;
    private final Map<String, String> params;

    /**
     * init standard scopes
     */
    private OAuthAuthorizationGrant() {
        params = new LinkedHashMap<>();

        params.put(SCOPE_PARAM_NAME, COMMON_SCOPES_PARAM);
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant, Set<String> scopesSet) {
        this(grant, scopesSet != null ? String.join(" ", scopesSet) : null);
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant, String scopes) {
        this();
        this.grant = grant;

        if (!StringHelper.isBlank(scopes)) {
            params.put(SCOPE_PARAM_NAME, params.get(SCOPE_PARAM_NAME) + SCOPES_DELIMITER + scopes);
        }
    }

    OAuthAuthorizationGrant(final AuthorizationGrant grant,
                                final Map<String, String> params) {
        this();
        this.grant = grant;
        if(params != null){
            this.params.putAll(params);
        }
    }

    @Override
    public Map<String, String> toParameters() {
        final Map<String, String> outParams = new LinkedHashMap<>();
        outParams.putAll(params);
        outParams.put("client_info", "1");
        outParams.putAll(grant.toParameters());

        return Collections.unmodifiableMap(outParams);
    }
    
    AuthorizationGrant getAuthorizationGrant() {
        return this.grant;
    }

    Map<String, String> getCustomParameters() {
        return params;
    }
}
