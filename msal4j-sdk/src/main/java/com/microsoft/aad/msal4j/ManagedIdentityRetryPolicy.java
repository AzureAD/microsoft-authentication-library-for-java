// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Retry policy for most Managed Identity scenarios
 */
class ManagedIdentityRetryPolicy implements IRetryPolicy {
    private static final int RETRY_NUM = 3;
    private static final int RETRY_DELAY_MS = 1000;

    private static int currentRetryDelayMs = RETRY_DELAY_MS;

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    HttpStatus.NOT_FOUND.getCode(),
                    HttpStatus.REQUEST_TIMEOUT.getCode(),
                    HttpStatus.TOO_MANY_REQUESTS.getCode(),
                    HttpStatus.INTERNAL_SERVER_ERROR.getCode(),
                    HttpStatus.SERVICE_UNAVAILABLE.getCode(),
                    HttpStatus.GATEWAY_TIMEOUT.getCode()
            ))
    );

    @Override
    public boolean isRetryable(IHttpResponse httpResponse) {
        return RETRYABLE_STATUS_CODES.contains(httpResponse.statusCode());
    }

    @Override
    public int getMaxRetryCount(IHttpResponse httpResponse) {
        return RETRY_NUM;
    }

    @Override
    public int getRetryDelayMs(IHttpResponse httpResponse) {
        return currentRetryDelayMs;
    }

    //Package-private methods to allow much quicker testing. The delay values should be treated as constants in any non-test scenario.
    static void setRetryDelayMs(int retryDelayMs) {
        currentRetryDelayMs = retryDelayMs;
    }

    static void resetToDefaults() {
        currentRetryDelayMs = RETRY_DELAY_MS;
    }
}