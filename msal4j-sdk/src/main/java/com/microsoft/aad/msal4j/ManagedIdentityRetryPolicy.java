// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Retry policy for most Managed Identity scenarios
 */
class ManagedIdentityRetryPolicy implements IRetryPolicy {
    private static final int RETRY_NUM = 3;
    private static int RETRY_DELAY_MS = 1000;

    @Override
    public boolean isRetryable(IHttpResponse httpResponse) {
        int statusCode = httpResponse.statusCode();

        return statusCode == 404 || // Not Found
                statusCode == 408 || // Request Timeout
                statusCode == 429 || // Too Many Requests
                statusCode == 500 || // Internal Server Error
                statusCode == 503 || // Service Unavailable
                statusCode == 504;   // Gateway Timeout
    }

    @Override
    public int getMaxRetryCount(IHttpResponse httpResponse) {
        return RETRY_NUM;
    }

    @Override
    public int getRetryDelayMs(IHttpResponse httpResponse) {
        return RETRY_DELAY_MS;
    }

    //Package-private methods to allow much quicker testing. The delay values should be treated as constants in any non-test scenario.
    static void setRetryDelayMs(int retryDelayMs) {
        RETRY_DELAY_MS = retryDelayMs;
    }

    static void resetToDefaults() {
        RETRY_DELAY_MS = 1000;
    }
}