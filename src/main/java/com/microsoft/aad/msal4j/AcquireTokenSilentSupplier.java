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

import java.util.Optional;
import java.util.Set;

class AcquireTokenSilentSupplier extends AuthenticationResultSupplier {

    private SilentRequest silentRequest;

    AcquireTokenSilentSupplier(ClientApplicationBase clientApplication, SilentRequest silentRequest) {
        super(clientApplication, silentRequest);

        this.silentRequest = silentRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        Authority requestAuthority = silentRequest.requestAuthority();
        if(requestAuthority.authorityType != AuthorityType.B2C){
            requestAuthority =
                    getAuthorityWithPrefNetworkHost(silentRequest.requestAuthority().authority());
        }

        AuthenticationResult res = clientApplication.tokenCache.getAuthenticationResult(
                silentRequest.parameters().account(),
                requestAuthority,
                silentRequest.parameters().scopes(),
                clientApplication.clientId());

        if (!silentRequest.parameters().forceRefresh() && !StringHelper.isBlank(res.accessToken())) {
            return res;
        }

        if (!StringHelper.isBlank(res.refreshToken())) {
            RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                    RefreshTokenParameters.builder(silentRequest.parameters().scopes(), res.refreshToken()).build(),
                    silentRequest.application(),
                    silentRequest.requestContext());

            AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                    new AcquireTokenByAuthorizationGrantSupplier(clientApplication, refreshTokenRequest, requestAuthority);

            return acquireTokenByAuthorisationGrantSupplier.execute();
        } else {
            return null;
        }
    }
}
