package com.microsoft.aad.msal4j;

/**
 * Exception type thrown when service returns throttling instruction:
 * Retry-After header, 429 or 5xx statuses.
 */
public class MsalThrottlingException extends MsalServiceException {

    private long retryInMs;

    /**
     * Constructor for MsalThrottlingException class
     *
     * @param retryInMs
     */
    public MsalThrottlingException(long retryInMs) {
        super("Request was throttled according to instructions from STS. Retry in " + retryInMs + " ms.",
                AuthenticationErrorCode.THROTTLED_REQUEST);

        this.retryInMs = retryInMs;
    }

    /**
     * how long to wait before repeating request
     */
    public long retryInMs() {
        return this.retryInMs;
    }
}
