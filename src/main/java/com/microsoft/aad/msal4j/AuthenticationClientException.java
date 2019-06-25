package com.microsoft.aad.msal4j;

/**
 *  Exception type thrown when and error occurs that is local to the library or the device.
 */
public class AuthenticationClientException extends AuthenticationException{

    /**
     * Initializes a new instance of the exception class with a specified error message
     * @param throwable the inner exception that is the cause of the current exception
     */
    public AuthenticationClientException(final Throwable throwable){
        super(throwable);
    }

    /**
     * Initializes a new instance of the exception class with a specified error message
     * @param message the error message that explains the reason for the exception
     */
    public AuthenticationClientException(final String message){
        super(message);
    }
}
