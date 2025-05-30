// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Default retry policy for most MSAL Java flows
 */
class DefaultRetryPolicy implements IRetryPolicy {
    private static final int RETRY_NUM = 1;
    private static int RETRY_DELAY_MS = 1000;

    @Override
    public boolean isRetryable(IHttpResponse httpResponse) {
        return HttpStatus.isServerError(httpResponse.statusCode()) &&
                HttpHelper.getRetryAfterHeader(httpResponse) == null;
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