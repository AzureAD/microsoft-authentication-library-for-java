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

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.net.URI;
import java.util.Set;

class AuthorizationCodeRequest extends MsalRequest {

    AuthorizationCodeRequest(Set<String> scopes,
                             String authorizationCode,
                             URI redirectUri,
                             ClientAuthentication clientAuthentication,
                             RequestContext requestContext){
        super(clientAuthentication, createMsalGrant(authorizationCode, redirectUri, scopes), requestContext);
    }

    private static AbstractMsalAuthorizationGrant createMsalGrant(
            String authorizationCode,
            URI redirectUri,
            Set<String> scopes){

        AuthorizationGrant authorizationGrant = new AuthorizationCodeGrant(
                new AuthorizationCode(authorizationCode), redirectUri);

        return new OAuthAuthorizationGrant(authorizationGrant, scopes);
    }
}
