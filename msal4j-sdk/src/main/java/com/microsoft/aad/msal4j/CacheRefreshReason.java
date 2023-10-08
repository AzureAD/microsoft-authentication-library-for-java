// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * List of possible reasons the tokens in an {@link IAuthenticationResult} were refreshed.
 */
public enum CacheRefreshReason {

    /**
     * Token did not need to be refreshed, or was retrieved in a non-silent call
     */
    NOT_APPLICABLE,
    /**
     * Silent call was made with the force refresh option
     */
    FORCE_REFRESH,
    /**
     * Access token was missing from the cache, but a valid refresh token was used to retrieve a new access token
     */
    NO_CACHED_ACCESS_TOKEN,
    /**
     * Cached access token was expired and successfully refreshed
     */
    EXPIRED,
    /**
     * Cached access token was not expired but was after the 'refresh_in' value, and was proactively refreshed before the expiration date
     */
    PROACTIVE_REFRESH
}
