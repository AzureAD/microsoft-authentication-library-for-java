// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

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

        AuthenticationResult res;

        if(silentRequest.parameters().account() == null){
            res = clientApplication.tokenCache.getCachedAuthenticationResult(
                    requestAuthority,
                    silentRequest.parameters().scopes(),
                    clientApplication.clientId());
            return StringHelper.isBlank(res.accessToken()) ? null : res;
        }

        res = clientApplication.tokenCache.getCachedAuthenticationResult(
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
