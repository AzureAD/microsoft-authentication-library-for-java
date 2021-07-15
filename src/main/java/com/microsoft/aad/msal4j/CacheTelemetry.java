// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Telemetry values covering the use of the cache in the library
 */
enum CacheTelemetry {
    /**
     * These values represent reasons why a token needed to be refreshed: either the flow does not use cached tokens (0),
     * the force refresh parameter was set (1), there was no cached access token (2), the cached access token expired (3),
     * or the cached token's refresh in time has passed (4)
     */
    REFRESH_CACHE_NOT_USED(0),
    REFRESH_FORCE_REFRESH(1),
    REFRESH_NO_ACCESS_TOKEN(2),
    REFRESH_ACCESS_TOKEN_EXPIRED(3),
    REFRESH_REFRESH_IN(4);

    final int telemetryValue;

    CacheTelemetry(int telemetryValue) {
        this.telemetryValue = telemetryValue;
    }
}
