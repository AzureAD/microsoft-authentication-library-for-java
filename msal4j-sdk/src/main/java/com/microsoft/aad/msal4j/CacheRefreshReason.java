// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Specifies the reason for fetching the access token from the identity provider when using {@link AbstractClientApplicationBase#acquireTokenSilently(SilentParameters)}
 */
public enum CacheRefreshReason {
    /**
     * Token did not need to be refreshed, or was retrieved in a non-silent call
     */
    NOT_APPLICABLE(0),
    /**
     * Silent call was made with the force refresh option, or claims were set
     */
    FORCE_REFRESH_OR_CLAIMS(1),
    /**
     * Access token was missing from the cache, but a valid refresh token was used to retrieve a new access token
     */
    NO_CACHED_ACCESS_TOKEN(2),
    /**
     * Cached access token was expired and successfully refreshed
     */
    EXPIRED(3),
    /**
     * Cached access token was not expired but was after the 'refresh_in' value, and was proactively refreshed before the expiration date
     */
    PROACTIVE_REFRESH(4);

    final int telemetryValue;

    CacheRefreshReason(int telemetryValue) {
        this.telemetryValue = telemetryValue;
    }
}
