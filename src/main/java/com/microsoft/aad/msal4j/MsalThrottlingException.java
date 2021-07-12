package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Exception type thrown when service returns throttling instruction:
 * Retry-After header, 429 or 5xx statuses.
 */
@Accessors(fluent = true)
@Getter
public class MsalThrottlingException extends MsalServiceException {

    /**
     * how long to wait before repeating request
     */
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
}
