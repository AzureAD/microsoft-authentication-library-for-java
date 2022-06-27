// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Date;

@Slf4j
class AcquireTokenSilentSupplier extends AuthenticationResultSupplier {

    private SilentRequest silentRequest;

    AcquireTokenSilentSupplier(AbstractClientApplicationBase clientApplication, SilentRequest silentRequest) {
        super(clientApplication, silentRequest);

        this.silentRequest = silentRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        Authority requestAuthority = silentRequest.requestAuthority();
        if (requestAuthority.authorityType != AuthorityType.B2C) {
            requestAuthority =
                    getAuthorityWithPrefNetworkHost(silentRequest.requestAuthority().authority());
        }

        AuthenticationResult res;
        if (silentRequest.parameters().account() == null) {
            res = clientApplication.tokenCache.getCachedAuthenticationResult(
                    requestAuthority,
                    silentRequest.parameters().scopes(),
                    clientApplication.clientId(),
                    silentRequest.assertion());
        } else {
            res = clientApplication.tokenCache.getCachedAuthenticationResult(
                    silentRequest.parameters().account(),
                    requestAuthority,
                    silentRequest.parameters().scopes(),
                    clientApplication.clientId());

            if (res == null) {
                throw new MsalClientException(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, AuthenticationErrorCode.CACHE_MISS);
            }

            if (!StringHelper.isBlank(res.accessToken())) {
                clientApplication.getServiceBundle().getServerSideTelemetry().incrementSilentSuccessfulCount();
            }

            //Determine if the current token needs to be refreshed according to the refresh_in value
            long currTimeStampSec = new Date().getTime() / 1000;
            boolean afterRefreshOn = res.refreshOn() != null && res.refreshOn() > 0 &&
                    res.refreshOn() < currTimeStampSec && res.expiresOn() >= currTimeStampSec;

            if (silentRequest.parameters().forceRefresh() || afterRefreshOn || StringHelper.isBlank(res.accessToken())) {

                //As of version 3 of the telemetry schema, there is a field for collecting data about why a token was refreshed,
                // so here we set the telemetry value based on the cause of the refresh
                if (silentRequest.parameters().forceRefresh()) {
                    clientApplication.getServiceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(
                            CacheTelemetry.REFRESH_FORCE_REFRESH.telemetryValue);
                } else if (afterRefreshOn) {
                    clientApplication.getServiceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(
                            CacheTelemetry.REFRESH_REFRESH_IN.telemetryValue);
                } else if (res.expiresOn() < currTimeStampSec) {
                    clientApplication.getServiceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(
                            CacheTelemetry.REFRESH_ACCESS_TOKEN_EXPIRED.telemetryValue);
                } else if (StringHelper.isBlank(res.accessToken())) {
                    clientApplication.getServiceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(
                            CacheTelemetry.REFRESH_NO_ACCESS_TOKEN.telemetryValue);
                }

                if (!StringHelper.isBlank(res.refreshToken())) {
                    //There are certain scenarios where the cached authority may differ from the client app's authority,
                    // such as when a request is instance aware. Unless overridden by SilentParameters.authorityUrl, the
                    // cached authority should be used in the token refresh request
                    if (silentRequest.parameters().authorityUrl() == null && !res.account().environment().equals(requestAuthority.host)) {
                        requestAuthority = Authority.createAuthority(new URL(requestAuthority.authority().replace(requestAuthority.host(),
                                res.account().environment())));
                    }

                    RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                            RefreshTokenParameters.builder(silentRequest.parameters().scopes(), res.refreshToken()).build(),
                            silentRequest.application(),
                            silentRequest.requestContext(),
                            silentRequest);

                    AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                            new AcquireTokenByAuthorizationGrantSupplier(clientApplication, refreshTokenRequest, requestAuthority);

                    try {
                        res = acquireTokenByAuthorisationGrantSupplier.execute();
                    } catch (MsalServiceException ex) {
                        //If the token refresh attempt threw a MsalServiceException but the refresh attempt was done
                        // only because of refreshOn, then simply return the existing cached token
                        if (afterRefreshOn && !(silentRequest.parameters().forceRefresh() || StringHelper.isBlank(res.accessToken()))) {
                            return res;
                        } else throw ex;
                    }
                } else {
                    res = null;
                }
            }
        }
        if (res == null || StringHelper.isBlank(res.accessToken())) {
            throw new MsalClientException(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, AuthenticationErrorCode.CACHE_MISS);
        }

        log.info("Returning token from cache");

        return res;
    }
}