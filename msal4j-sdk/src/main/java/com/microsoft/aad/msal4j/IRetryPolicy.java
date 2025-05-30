package com.microsoft.aad.msal4j;

/**
 * Interface for HTTP request retry policies
 */
 interface IRetryPolicy {
    /**
     * Determines whether a request should be retried based on the HTTP response
     * @param httpResponse The HTTP response to evaluate
     * @return true if retry should be attempted, false otherwise
     */
    boolean isRetryable(IHttpResponse httpResponse);

    /**
     * Gets the number of retries to attempt based on the HTTP response
     * @return maximum retry count
     */
    int getMaxRetryCount(IHttpResponse httpResponse);

    /**
     * Gets the delay in milliseconds between retry attempts
     * @return delay in milliseconds
     */
    int getRetryDelayMs(IHttpResponse httpResponse);
}