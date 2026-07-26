// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import javax.net.ssl.SSLException;

/**
 * Default policy for retrying HTTP requests that failed due to a network-level exception.
 */
class DefaultRetryableExceptionPolicy implements IRetryableExceptionPolicy {
    private static final int RETRY_NUM = 1;
    private static final int RETRY_DELAY_MS = 1000;

    @Override
    public boolean isRetryable(Exception exception) {
        if (exception instanceof SSLException || exception instanceof UnknownHostException) {
            return false;
        }
        return exception instanceof SocketException || exception instanceof SocketTimeoutException;
    }

    @Override
    public int getMaxRetryCount(Exception exception) {
        return RETRY_NUM;
    }

    @Override
    public int getRetryDelayMs(Exception exception) {
        return RETRY_DELAY_MS;
    }
}
