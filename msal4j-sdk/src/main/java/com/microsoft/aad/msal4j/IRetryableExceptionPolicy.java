// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface for policies deciding how HTTP request failures caused by exceptions
 * (e.g. network-level {@link java.io.IOException}s) should be retried.
 * <p>
 * This complements {@link IRetryPolicy}, which handles retries based on HTTP responses.
 * Implementations decide whether a given exception represents a transient failure worth
 * retrying, as opposed to a permanent error (e.g. TLS/certificate misconfiguration).
 */
interface IRetryableExceptionPolicy {
    /**
     * Determines whether a request should be retried based on the exception thrown.
     *
     * @param exception The exception thrown while attempting the request
     * @return true if retry should be attempted, false otherwise
     */
    boolean isRetryable(Exception exception);

    /**
     * Gets the maximum number of retries to attempt based on the exception thrown.
     *
     * @param exception The exception thrown while attempting the request
     * @return maximum retry count for this specific exception
     */
    int getMaxRetryCount(Exception exception);

    /**
     * Gets the delay in milliseconds to wait before the next retry attempt.
     *
     * @param exception The exception thrown while attempting the request
     * @return delay in milliseconds before attempting the next retry
     */
    int getRetryDelayMs(Exception exception);
}
