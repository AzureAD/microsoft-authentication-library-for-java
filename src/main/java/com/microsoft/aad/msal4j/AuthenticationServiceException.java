// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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
