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
import com.nimbusds.oauth2.sdk.JWTBearerGrant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class MsalOAuthAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private AuthorizationGrant grant;
    private final Map<String, String> params;


    MsalOAuthAuthorizationGrant(final AuthorizationGrant grant, Set<String> scopes) {
        this.grant = grant;
        this.params = convertScopesToParameters(scopes);
    }

    MsalOAuthAuthorizationGrant(final AuthorizationGrant grant,
                                final Map<String, String> params) {
        this.grant = grant;

        this.params = initializeStandardParamaters();
        if(params != null){
            this.params.putAll(params);
        }
    }

    @Override
    public Map<String, String> toParameters() {
        final Map<String, String> outParams = new LinkedHashMap<String, String>();
        outParams.putAll(params);
        outParams.putAll(grant.toParameters());

        return Collections.unmodifiableMap(outParams);
    }


    private Map<String, String> convertScopesToParameters(Set<String> scopes){
        Map<String, String> parameters = initializeStandardParamaters();

        String scopesStr = scopes != null ? String.join(" ", scopes) : null;
        if (!StringHelper.isBlank(scopesStr)) {
            parameters.put(SCOPE_PARAM_NAME, parameters.get(SCOPE_PARAM_NAME) + SCOPES_DELIMITER + scopesStr);
        }

        if(grant instanceof JWTBearerGrant){
            parameters.put("requested_token_use", "on_behalf_of");
        }
        return parameters;
    }


    private Map<String, String> initializeStandardParamaters(){
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put(SCOPE_PARAM_NAME, COMMON_SCOPES_PARAM);

        return parameters;
    }

    AuthorizationGrant getAuthorizationGrant() {
        return this.grant;
    }

    Map<String, String> getCustomParameters() {
        return params;
    }
}
