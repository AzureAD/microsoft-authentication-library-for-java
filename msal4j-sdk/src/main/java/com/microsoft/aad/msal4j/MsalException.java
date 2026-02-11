// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Base exception type thrown when an error occurs during token acquisition.
 */
public class MsalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Authentication error code
     */
    private String errorCode;

    /**
     * Correlation ID for request tracking
     */
    private String correlationId;

    /**
     * Initializes a new instance of the exception class
     *
     * @param throwable the inner exception that is the cause of the current exception
     */
    public MsalException(final Throwable throwable) {
        super(throwable);
    }

    /**
     * Initializes a new instance of the exception class
     *
     * @param message the error message that explains the reason for the exception
     */
    public MsalException(final String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Initializes a new instance of the exception class with correlation ID
     *
     * @param message the error message that explains the reason for the exception
     * @param errorCode the error code
     * @param correlationId the correlation ID for request tracking
     */
    public MsalException(final String message, String errorCode, String correlationId) {
        super(LogHelper.createMessage(message, correlationId));
        this.errorCode = errorCode;
        this.correlationId = correlationId;
    }

    public String errorCode() {
        return this.errorCode;
    }

    public String correlationId() {
        return this.correlationId;
    }
}
