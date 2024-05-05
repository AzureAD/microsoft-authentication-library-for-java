// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;


import lombok.extern.slf4j.Slf4j;

import java.net.URL;
import java.util.Date;

@Slf4j
class AcquireTokenSilentSupplier extends AuthenticationResultSupplier {

    private SilentRequest silentRequest;
    private boolean shouldRefresh;
    private boolean afterRefreshOn;

    AcquireTokenSilentSupplier(AbstractApplicationBase clientApplication, SilentRequest silentRequest) {
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

            //Some cached tokens were found, but this metadata will be overwritten if token needs to be refreshed
            res.metadata().tokenSource(TokenSource.CACHE);

            if (!StringHelper.isBlank(res.accessToken())) {
                clientApplication.serviceBundle().getServerSideTelemetry().incrementSilentSuccessfulCount();
            }

            shouldRefresh = shouldRefresh(silentRequest.parameters(), res);

            if (shouldRefresh || afterRefreshOn) {
                if (!StringHelper.isBlank(res.refreshToken())) {
                    //There are certain scenarios where the cached authority may differ from the client app's authority,
                    // such as when a request is instance aware. Unless overridden by SilentParameters.authorityUrl, the
                    // cached authority should be used in the token refresh request
                    if (silentRequest.parameters().authorityUrl() == null && !res.account().environment().equals(requestAuthority.host)) {
                        requestAuthority = Authority.createAuthority(new URL(requestAuthority.authority().replace(requestAuthority.host(),
                                res.account().environment())));
                    }
                    res = makeRefreshRequest(res, requestAuthority);
                } else {
                    res = null;
                }
            }
        }
        if (res == null || StringHelper.isBlank(res.accessToken())) {
            throw new MsalClientException(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, AuthenticationErrorCode.CACHE_MISS);
        }

        log.debug("Returning token from cache");

        return res;
    }

    private AuthenticationResult makeRefreshRequest(AuthenticationResult cachedResult,  Authority requestAuthority) throws Exception {
        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                RefreshTokenParameters.builder(silentRequest.parameters().scopes(), cachedResult.refreshToken()).build(),
                silentRequest.application(),
                silentRequest.requestContext(),
                silentRequest);

        AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                new AcquireTokenByAuthorizationGrantSupplier(clientApplication, refreshTokenRequest, requestAuthority);

        try {
            AuthenticationResult refreshedResult = acquireTokenByAuthorisationGrantSupplier.execute();

            refreshedResult.metadata().tokenSource(TokenSource.IDENTITY_PROVIDER);

            log.info("Access token refreshed successfully.");
            return refreshedResult;
        } catch (MsalServiceException ex) {
            //If the token refresh attempt threw a MsalServiceException but the refresh attempt was done
            // only because of refreshOn, then simply return the existing cached token
            if (!afterRefreshOn && (silentRequest.parameters().forceRefresh() || StringHelper.isBlank(cachedResult.accessToken()))) {
                throw ex;
            }
            return cachedResult;
        }
    }

    //Handles any logic to determine if a token should be refreshed, based on the request parameters and the status of cached tokens
    private boolean shouldRefresh(SilentParameters parameters, AuthenticationResult cachedResult) {

        //If forceRefresh is true, no reason to check any other option
        if (parameters.forceRefresh()) {
            setCacheTelemetry(CacheTelemetry.REFRESH_FORCE_REFRESH.telemetryValue);
            return true;
        }

        //If the request contains claims then the token should be refreshed, to ensure that the returned token has the correct claims
        //  Note: these are the types of claims found in (for example) a claims challenge, and do not include client capabilities
        if (parameters.claims() != null) {
            setCacheTelemetry(CacheTelemetry.REFRESH_FORCE_REFRESH.telemetryValue);
            return true;
        }

        long currTimeStampSec = new Date().getTime() / 1000;

        if (cachedResult.expiresOn() < currTimeStampSec) {
            setCacheTelemetry(CacheTelemetry.REFRESH_ACCESS_TOKEN_EXPIRED.telemetryValue);
            return true;
        }

        //Certain long-lived tokens will have a 'refresh on' time that indicates a refresh should be attempted long before the token would expire
        if (cachedResult.refreshOn() != null && cachedResult.refreshOn() > 0 &&
                cachedResult.refreshOn() < currTimeStampSec && cachedResult.expiresOn() >= currTimeStampSec){
            setCacheTelemetry(CacheTelemetry.REFRESH_REFRESH_IN.telemetryValue);
            afterRefreshOn = true;
            return true;
        }

        //If there is a refresh token but no access token, we should use the refresh token to get the access token
        if (StringHelper.isBlank(cachedResult.accessToken()) && !StringHelper.isBlank(cachedResult.refreshToken())) {
            setCacheTelemetry(CacheTelemetry.REFRESH_NO_ACCESS_TOKEN.telemetryValue);
            return true;
        }

        return false;
    }

    private void setCacheTelemetry(int cacheInfoValue){
        clientApplication.serviceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(cacheInfoValue);
    }
}