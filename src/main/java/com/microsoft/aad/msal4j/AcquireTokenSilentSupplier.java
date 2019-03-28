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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

class AcquireTokenSilentSupplier extends AuthenticationResultSupplier {

    private Account account;
    private Set<String> scopes;
    private AuthenticationAuthority requestAuthority;
    private boolean forceRefresh;
    private ClientAuthentication clientAuthentication;

    AcquireTokenSilentSupplier(ClientApplicationBase clientApplication, ClientAuthentication clientAuthentication,
                               Account account, Set<String> scopes, String authorityUrl, boolean forceRefresh)
            throws MalformedURLException {
        super(clientApplication, new ClientDataHttpHeaders(clientApplication.getCorrelationId()));
        this.clientApplication = clientApplication;
        this.clientAuthentication = clientAuthentication;

        this.account = account;
        this.scopes = scopes;

        if (!StringHelper.isBlank(authorityUrl)) {
            requestAuthority = new AuthenticationAuthority(new URL(authorityUrl));
        } else {
            requestAuthority = clientApplication.authenticationAuthority;
        }
        this.forceRefresh = forceRefresh;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        requestAuthority = getAuthorityWithPrefNetworkHost(requestAuthority.getAuthority());

        AuthenticationResult res =
                clientApplication.tokenCache.getAuthenticationResult
                        (account, requestAuthority, scopes, clientApplication.clientId);

        if (!forceRefresh && !StringHelper.isBlank(res.accessToken())) {
            return res;
        }

        if (StringHelper.isBlank(res.refreshToken())) {
            return null;
        } else {
            RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                    res.refreshToken(),
                    scopes,
                    clientAuthentication,
                    new RequestContext(clientApplication.clientId, clientApplication.getCorrelationId()));

            AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                    new AcquireTokenByAuthorizationGrantSupplier(clientApplication, refreshTokenRequest, requestAuthority);

            return acquireTokenByAuthorisationGrantSupplier.execute();
        }
    }
}
