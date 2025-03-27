// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Exception type thrown when service returns an error response or other networking errors occur.
 */
public class MsalServiceException extends MsalException {

    private Integer statusCode;
    private String statusMessage;
    private String correlationId;
    private String claims;
    private Map<String, List<String>> headers;
    private String managedIdentitySource;
    private String subError;

    /**
     * Initializes a new instance of the exception class with a specified error message
     *
     * @param message the error message that explains the reason for the exception
     * @param error a simplified error code from {@link AuthenticationErrorCode} and used for references in documentation
     */
    public MsalServiceException(final String message, final String error) {
        super(message, error);
    }

    /**
     * Initializes a new instance of the exception class
     *
     * @param errorResponse response object contain information about error returned by server
     * @param httpHeaders   http headers from the server response
     */
    public MsalServiceException(
            final ErrorResponse errorResponse,
            final Map<String, List<String>> httpHeaders) {

        super(errorResponse.errorDescription, errorResponse.error());
        this.statusCode = errorResponse.statusCode();
        this.statusMessage = errorResponse.statusMessage();
        this.subError = errorResponse.subError();
        this.correlationId = errorResponse.correlation_id();
        this.claims = errorResponse.claims();
        this.headers = Collections.unmodifiableMap(httpHeaders);
    }

    /**
     * Initializes a new instance of the exception class, with any extra properties for a Managed Identity error
     *
     * @param message the error message that explains the reason for the exception
     * @param managedIdentitySource the Managed Identity service
     */
    public MsalServiceException(
            final String message, final String error,
            ManagedIdentitySourceType managedIdentitySource) {
        this(message, error); //Call the more common constructor to set the error message properties

        this.managedIdentitySource = managedIdentitySource.name();
    }


    /**
     * Initializes a new instance of the exception class
     *
     * @param discoveryResponse response object from instance discovery network call
     */
    public MsalServiceException(final AadInstanceDiscoveryResponse discoveryResponse) {
        super(discoveryResponse.errorDescription(), discoveryResponse.error());

        this.correlationId = discoveryResponse.correlationId();
    }

    /**
     * Status code returned from http layer
     */
    public Integer statusCode() {
        return this.statusCode;
    }

    /**
     * Status message returned from the http layer
     */
    public String statusMessage() {
        return this.statusMessage;
    }

    /**
     * An ID that can be used to piece up a single authentication flow.
     */
    public String correlationId() {
        return this.correlationId;
    }

    /**
     * Claims included in the claims challenge
     */
    public String claims() {
        return this.claims;
    }

    /**
     * Contains the http headers from the server response that indicated an error.
     * When the server returns a 429 Too Many Requests error, a Retry-After should be set.
     * It is important to read and respect the time specified in the Retry-After header
     */
    public Map<String, List<String>> headers() {
        return this.headers;
    }

    public String managedIdentitySource() {
        return this.managedIdentitySource;
    }

    String subError() {
        return this.subError;
    }
}
