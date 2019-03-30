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

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Set;

@Accessors(fluent = true) @Getter
public class SilentRequest extends MsalRequest {

    private Account account;
    private Set<String> scopes;
    private boolean forceRefresh;
    private AuthenticationAuthority requestAuthority;

    SilentRequest(Set<String> scopes,
                  AuthenticationAuthority requestAuthority,
                  boolean forceRefresh,
                  Account account,
                  ClientAuthentication clientAuthentication,
                  RequestContext requestContext){

        super(clientAuthentication, null, requestContext);

        this.account = account;
        this.scopes = scopes;
        this.requestAuthority = requestAuthority;
        this.forceRefresh = forceRefresh;
        /*
        this.forceRefresh = forceRefresh;
        if (!StringHelper.isBlank(authorityUrl)) {
            requestAuthority = new AuthenticationAuthority(new URL(authorityUrl));
        } else {
            requestAuthority = clientApplication.authenticationAuthority;
        }
        */
    }
}
