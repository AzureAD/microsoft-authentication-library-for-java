// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

//IMDS uses a different try policy than other MI flows, see https://github.com/AzureAD/microsoft-authentication-library-for-dotnet/blob/main/docs/imds_retry_based_on_errors.md
class IMDSRetryPolicy extends ManagedIdentityRetryPolicy {
    private static final int LINEAR_RETRY_NUM = 7;
    private static int LINEAR_RETRY_DELAY_MS = 10000; // 10 seconds
    private static final int EXPONENTIAL_RETRY_NUM = 3;
    private static int EXPONENTIAL_RETRY_DELAY_MS = 1000; // 1 second

    private int currentRetryCount;
    private int lastStatusCode;
    
    @Override
    public boolean isRetryable(IHttpResponse httpResponse) {
        currentRetryCount++;
        lastStatusCode = httpResponse.statusCode();

        return HttpStatus.isServerError(lastStatusCode) ||
                lastStatusCode == HttpStatus.NOT_FOUND.getCode() ||
                lastStatusCode == HttpStatus.REQUEST_TIMEOUT.getCode() ||
                lastStatusCode == HttpStatus.GONE.getCode() ||
                lastStatusCode == HttpStatus.TOO_MANY_REQUESTS.getCode();
    }

    @Override
    public int getMaxRetryCount(IHttpResponse httpResponse) {
        return (httpResponse.statusCode() == 410) ? LINEAR_RETRY_NUM : EXPONENTIAL_RETRY_NUM;
    }

    @Override
    public int getRetryDelayMs(IHttpResponse httpResponse) {
        // Use exponential backoff for non-410 status codes
        if (lastStatusCode == 410) {
            return LINEAR_RETRY_DELAY_MS;
        } else {
            return (int) (Math.pow(2, currentRetryCount) * EXPONENTIAL_RETRY_DELAY_MS);
        }
    }

    //Package-private methods to allow much quicker testing. The delay values should be treated as constants in any non-test scenario.
    static void setRetryDelayMs(int retryDelayMs) {
        LINEAR_RETRY_DELAY_MS = retryDelayMs;
        EXPONENTIAL_RETRY_DELAY_MS = retryDelayMs;
    }

    static void resetToDefaults() {
        LINEAR_RETRY_DELAY_MS = 10000;
        EXPONENTIAL_RETRY_DELAY_MS = 1000;
    }
}
