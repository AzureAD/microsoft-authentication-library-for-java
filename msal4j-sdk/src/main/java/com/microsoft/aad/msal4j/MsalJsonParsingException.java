// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * This exception class informs developers that a response contained invalid JSON or otherwise could not be parsed
 */
public class MsalJsonParsingException extends MsalServiceException {

    /**
     * Initializes a new instance of the exception class with a specified error message
     *
     * @param message the error message that explains the reason for the exception
     * @param error a simplified error code from {@link AuthenticationErrorCode} and used for references in documentation
     */
    MsalJsonParsingException(final String message, final String error) {
        super(message, error);
    }

    /**
     * Initializes a new instance of the exception class, with extra properties for a Managed Identity error
     *
     * @param message the error message that explains the reason for the exception
     * @param error a simplified error code
     * @param managedIdentitySource the Managed Identity service
     */
    MsalJsonParsingException(
            final String message, final String error,
            ManagedIdentitySourceType managedIdentitySource) {
        super(message, error, managedIdentitySource);
    }

}
