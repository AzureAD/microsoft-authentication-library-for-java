// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

//IMDS uses a different try policy than other MI flows, see https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/blob/main/docs/imds_retry_based_on_errors.md
class IMDSRetryPolicy extends ManagedIdentityRetryPolicy {
    private static final int LINEAR_RETRY_NUM = 7;
    private static final int LINEAR_RETRY_DELAY_MS = 10000; // 10 seconds
    private static final int EXPONENTIAL_RETRY_NUM = 3;
    private static final int EXPONENTIAL_RETRY_DELAY_MS = 1000; // 1 second

    private static int currentLinearRetryDelayMs = LINEAR_RETRY_DELAY_MS;
    private static int exponentialLinearRetryDelayMs = EXPONENTIAL_RETRY_DELAY_MS;

    private int currentRetryCount;
    private int lastStatusCode;

    private static final Set<Integer> RETRYABLE_STATUS_CODES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    HttpStatus.HTTP_NOT_FOUND,
                    HttpStatus.HTTP_REQUEST_TIMEOUT,
                    HttpStatus.HTTP_GONE,
                    HttpStatus.HTTP_TOO_MANY_REQUESTS
            ))
    );

    @Override
    /**
     * TODO: Add description
     */
    public boolean isRetryable(IHttpResponse httpResponse) {
        currentRetryCount++;
        lastStatusCode = httpResponse.statusCode();

        return HttpStatus.isServerError(lastStatusCode) || RETRYABLE_STATUS_CODES.contains(lastStatusCode);
    }

    @Override
    /**
     * TODO: Add description
     */
    public int getMaxRetryCount(IHttpResponse httpResponse) {
        return (httpResponse.statusCode() == HttpStatus.HTTP_GONE) ? LINEAR_RETRY_NUM : EXPONENTIAL_RETRY_NUM;
    }

    @Override
    /**
     * TODO: Add description
     */
    public int getRetryDelayMs(IHttpResponse httpResponse) {
        // Use exponential backoff for non-410 status codes
        if (lastStatusCode == HttpStatus.HTTP_GONE) {
            return currentLinearRetryDelayMs;
        } else {
            return (int) (Math.pow(2, currentRetryCount) * exponentialLinearRetryDelayMs);
        }
    }

    //Package-private methods to allow much quicker testing. The delay values should be treated as constants in any non-test scenario.
    static void setRetryDelayMs(int retryDelayMs) {
        currentLinearRetryDelayMs = retryDelayMs;
        exponentialLinearRetryDelayMs = retryDelayMs;
    }

    static void resetToDefaults() {
        currentLinearRetryDelayMs = LINEAR_RETRY_DELAY_MS;
        exponentialLinearRetryDelayMs = EXPONENTIAL_RETRY_DELAY_MS;
    }
}
