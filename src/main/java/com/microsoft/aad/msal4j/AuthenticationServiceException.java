package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * Exception type thrown when service returns an error response or other networking errors occur.
 */
@Accessors(fluent = true)
@Getter
public class AuthenticationServiceException extends AuthenticationException{

    /**
     * The protocol error code returned by the service
     */
    private String errorCode;

    /**
     * More specific error
     */
    private String subError;

    /**
     * Status code returned from http layer
     */
    private Integer statusCode;

    /**
     * Status message returned from the http layer
     */
    private String statusMessage;

    /**
     * An ID that can used to piece up a single authentication flow.
     */
    private String correlationId;

    /**
     * Claims included in the claims challenge
     */
    private String claims;

    /**
     * Contains the http headers from the server response that indicated an error.
     * When the server returns a 429 Too Many Requests error, a Retry-After should be set.
     * It is important to read and respect the time specified in the Retry-After header
     */
    private Map<String, List<String>> headers;

    /**
     * Initializes a new instance of the exception class with a specified error message
     * @param message the error message that explains the reason for the exception
     */
    public AuthenticationServiceException(final String message){
        super(message);
    }

    /**
     * Initializes a new instance of the exception class
     * @param errorResponse response object contain information about error returned by server
     * @param httpHeaders http headers from the server response
     */
    public AuthenticationServiceException(
            final ErrorResponse errorResponse,
            final Map<String, List<String>> httpHeaders) {

        super(errorResponse.errorDescription);

        this.errorCode = errorResponse.error();
        this.statusCode = errorResponse.statusCode();
        this.statusMessage = errorResponse.statusMessage();
        this.subError = errorResponse.subError();
        this.correlationId = errorResponse.correlation_id();
        this.claims = errorResponse.claims();
        this.headers =  httpHeaders;
    }

    /**
     * Initializes a new instance of the exception class
     * @param discoveryResponse response object from instance discovery network call
     */
    public AuthenticationServiceException(InstanceDiscoveryResponse discoveryResponse){

        super(discoveryResponse.errorDescription());

        this.errorCode = discoveryResponse.error();
        this.correlationId = discoveryResponse.correlationId();
    }
}
