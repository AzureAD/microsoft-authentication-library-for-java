// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface for HTTP request retry policies used by the library.
 * <p>
 * Retry policies define when and how HTTP requests should be retried in case of failures or timeouts,
 * helping to increase reliability of network communications.
 */
 interface IRetryPolicy {
    /**
     * Determines whether a request should be retried based on the HTTP response.
     * <p>
     * Implementations can examine status codes, headers, or other response attributes
     * to decide if another attempt should be made.
     *
     * @param httpResponse The HTTP response to evaluate
     * @return true if retry should be attempted, false otherwise
     */
    boolean isRetryable(IHttpResponse httpResponse);

    /**
     * Gets the maximum number of retries to attempt based on the HTTP response.
     * <p>
     * The implementation can return different retry counts for different types of failures.
     *
     * @param httpResponse The HTTP response to evaluate
     * @return maximum retry count for this specific response
     */
    int getMaxRetryCount(IHttpResponse httpResponse);

    /**
     * Gets the delay in milliseconds to wait before the next retry attempt.
     * <p>
     * Implementations may use different backoff strategies such as fixed, exponential,
     * or jittered delays based on the response type or retry count.
     *
     * @param httpResponse The HTTP response to evaluate
     * @return delay in milliseconds before attempting the next retry
     */
    int getRetryDelayMs(IHttpResponse httpResponse);
}