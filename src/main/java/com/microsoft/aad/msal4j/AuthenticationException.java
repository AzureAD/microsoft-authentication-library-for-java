// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * MSAL generic exception class
 */
public class AuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Accessors(fluent = true)
    @Getter
    private AuthenticationErrorCode errorCode;

    /**
     * Constructor
     *
     * @param t Throwable object
     */
    public AuthenticationException(final Throwable t) {
        super(t);

        this.errorCode = AuthenticationErrorCode.UNKNOWN;
    }

    /**
     * Constructor
     *
     * @param message string error message
     */
    public AuthenticationException(final String message) {
        this(AuthenticationErrorCode.UNKNOWN, message);
    }

    public AuthenticationException(AuthenticationErrorCode errorCode, final String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructor
     *
     * @param message string error message
     * @param t Throwable object
     */
    public AuthenticationException(final String message, final Throwable t) {
        super(message, t);

        this.errorCode = AuthenticationErrorCode.UNKNOWN;
    }
}
