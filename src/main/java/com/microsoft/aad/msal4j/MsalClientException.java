// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 *  Exception type thrown when and error occurs that is local to the library or the device.
 */
public class MsalClientException extends MsalException {

    /**
     * Initializes a new instance of the exception class with a instance of Throwable
     * @param throwable the inner exception that is the cause of the current exception
     */
    public MsalClientException(final Throwable throwable){
        super(throwable);
    }

    /**
     * Initializes a new instance of the exception class with a specified error message
     * @param message the error message that explains the reason for the exception
     */
    public MsalClientException(final String message, final String errorCode){
        super(message, errorCode);
    }
}
