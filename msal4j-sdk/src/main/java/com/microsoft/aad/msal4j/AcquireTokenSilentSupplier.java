// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Date;

class AcquireTokenSilentSupplier extends AuthenticationResultSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenSilentSupplier.class);
    private SilentRequest silentRequest;
    protected static final int ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC = 5 * 60;

    AcquireTokenSilentSupplier(AbstractApplicationBase clientApplication, SilentRequest silentRequest) {
        super(clientApplication, silentRequest);

        this.silentRequest = silentRequest;
    }

    @Override
    AuthenticationResult execute() throws Exception {
        boolean shouldRefresh;
        Authority requestAuthority = getAuthorityWithPrefNetworkHost(silentRequest.requestAuthority().authority());

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

            if (shouldRefresh) {
                if (!StringHelper.isBlank(res.refreshToken())) {
                    //There are certain scenarios where the cached authority may differ from the client app's authority,
                    // such as when a request is instance aware. Unless overridden by SilentParameters.authorityUrl, the
                    // cached authority should be used in the token refresh request
                    if (silentRequest.parameters().authorityUrl() == null && !res.account().environment().equals(requestAuthority.host)) {
                        requestAuthority = Authority.createAuthority(new URL(requestAuthority.authority().replace(requestAuthority.host(),
                                res.account().environment())));
                    }

                    res = makeRefreshRequest(res, requestAuthority, clientApplication.serviceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo());
                } else {
                    res = null;
                }
            }
        }

        if (res == null || StringHelper.isBlank(res.accessToken())) {
            throw new MsalClientException(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, AuthenticationErrorCode.CACHE_MISS);
        }

        LOG.debug("Returning token from cache");

        return res;
    }

    private AuthenticationResult makeRefreshRequest(AuthenticationResult cachedResult, Authority requestAuthority, CacheRefreshReason refreshReason) throws Exception {

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                RefreshTokenParameters.builder(silentRequest.parameters().scopes(), cachedResult.refreshToken()).build(),
                silentRequest.application(),
                silentRequest.requestContext(),
                silentRequest);

        //The ServiceBundle will have a new CurrentRequest object when the RefreshTokenRequest is made, so the telemetry value needs to be set again
        setCacheTelemetry(refreshReason);

        AcquireTokenByAuthorizationGrantSupplier acquireTokenByAuthorisationGrantSupplier =
                new AcquireTokenByAuthorizationGrantSupplier(clientApplication, refreshTokenRequest, requestAuthority);

        try {
            AuthenticationResult refreshedResult = acquireTokenByAuthorisationGrantSupplier.execute();

            refreshedResult.metadata().tokenSource(TokenSource.IDENTITY_PROVIDER);
            refreshedResult.metadata().cacheRefreshReason(refreshReason);

            LOG.info("Access token refreshed successfully.");
            return refreshedResult;
        } catch (MsalServiceException ex) {
            //If the token refresh attempt threw a MsalServiceException but the refresh attempt was done
            // only because of refreshOn, then simply return the existing cached token rather than throw an exception
            if (refreshReason == CacheRefreshReason.PROACTIVE_REFRESH) {
                return cachedResult;
            }
            throw ex;
        }
    }

    //Handles any logic to determine if a token should be refreshed, based on the request parameters and the status of cached tokens
    private boolean shouldRefresh(SilentParameters parameters, AuthenticationResult cachedResult) {

        //If forceRefresh is true, no reason to check any other option
        if (parameters.forceRefresh()) {
            setCacheTelemetry(CacheRefreshReason.FORCE_REFRESH);
            LOG.debug("Refreshing access token. Cache refresh reason: {}", CacheRefreshReason.FORCE_REFRESH);
            return true;
        }

        //If the request contains claims then the token should be refreshed, to ensure that the returned token has the correct claims
        //  Note: these are the types of claims found in (for example) a claims challenge, and do not include client capabilities
        if (parameters.claims() != null) {
            setCacheTelemetry(CacheRefreshReason.CLAIMS);
            LOG.debug("Refreshing access token. Cache refresh reason: {}", CacheRefreshReason.CLAIMS);
            return true;
        }

        long currTimeStampSec = new Date().getTime() / 1000;

        //If the access token is expired or within 5 minutes of becoming expired, refresh it
        if (!StringHelper.isBlank(cachedResult.accessToken()) && cachedResult.expiresOn() < (currTimeStampSec + ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC)) {
            setCacheTelemetry(CacheRefreshReason.EXPIRED);
            LOG.debug("Refreshing access token. Cache refresh reason: {}", CacheRefreshReason.EXPIRED);
            return true;
        }

        //Certain long-lived tokens will have a 'refresh on' time that indicates a refresh should be attempted long before the token would expire
        if (!StringHelper.isBlank(cachedResult.accessToken()) &&
                cachedResult.refreshOn() != null && cachedResult.refreshOn() > 0 &&
                cachedResult.refreshOn() < currTimeStampSec && cachedResult.expiresOn() >= (currTimeStampSec + ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC)){
            setCacheTelemetry(CacheRefreshReason.PROACTIVE_REFRESH);
            LOG.debug("Refreshing access token. Cache refresh reason: {}", CacheRefreshReason.PROACTIVE_REFRESH);
            return true;
        }

        //If there is a refresh token but no access token, we should use the refresh token to get the access token
        if (StringHelper.isBlank(cachedResult.accessToken()) && !StringHelper.isBlank(cachedResult.refreshToken())) {
            setCacheTelemetry(CacheRefreshReason.NO_CACHED_ACCESS_TOKEN);
            LOG.debug("Refreshing access token. Cache refresh reason: {}", CacheRefreshReason.NO_CACHED_ACCESS_TOKEN);
            return true;
        }

        return false;
    }

    private void setCacheTelemetry(CacheRefreshReason cacheInfoValue){
        clientApplication.serviceBundle().getServerSideTelemetry().getCurrentRequest().cacheInfo(cacheInfoValue);
    }
}