// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Date;
import org.slf4j.Logger;

class SilentRequestHelper {

    private static final int ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC = 5 * 60;

    private SilentRequestHelper() {
        // Utility class
    }

    static CacheRefreshReason NeedsRefresh(SilentParameters parameters, AuthenticationResult cachedResult, Logger log) {
        //If forceRefresh is true, no reason to check any other option
        if (parameters.forceRefresh()) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.FORCE_REFRESH));
            return CacheRefreshReason.FORCE_REFRESH;
        }

        //If the request contains claims then the token should be refreshed, to ensure that the returned token has the correct claims
        //  Note: these are the types of claims found in (for example) a claims challenge, and do not include client capabilities
        if (parameters.claims() != null) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.CLAIMS));
            return CacheRefreshReason.CLAIMS;
        }

        long currTimeStampSec = new Date().getTime() / 1000;

        //If the access token is expired or within 5 minutes of becoming expired, refresh it
        if (!StringHelper.isBlank(cachedResult.accessToken()) && cachedResult.expiresOn() < (currTimeStampSec + ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC)) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.EXPIRED));
            return CacheRefreshReason.EXPIRED;
        }

        //Certain long-lived tokens will have a 'refresh on' time that indicates a refresh should be attempted long before the token would expire
        if (!StringHelper.isBlank(cachedResult.accessToken()) &&
                cachedResult.refreshOn() != null && cachedResult.refreshOn() > 0 &&
                cachedResult.refreshOn() < currTimeStampSec && cachedResult.expiresOn() >= (currTimeStampSec + ACCESS_TOKEN_EXPIRE_BUFFER_IN_SEC)){
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.PROACTIVE_REFRESH));
            return CacheRefreshReason.PROACTIVE_REFRESH;
        }

        //If there is a refresh token but no access token, we should use the refresh token to get the access token
        if (StringHelper.isBlank(cachedResult.accessToken()) && !StringHelper.isBlank(cachedResult.refreshToken())) {
            log.debug(String.format("Refreshing access token. Cache refresh reason: %s", CacheRefreshReason.NO_CACHED_ACCESS_TOKEN));
            return CacheRefreshReason.NO_CACHED_ACCESS_TOKEN;
        }

        return CacheRefreshReason.NOT_APPLICABLE;
    }
}
