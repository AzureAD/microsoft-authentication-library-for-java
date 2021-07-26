// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Base exception type thrown when an error occurs during token acquisition.
 */
public class MsalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Authentication error code
     */
    @Accessors(fluent = true)
    @Getter
    private String errorCode;

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
}
