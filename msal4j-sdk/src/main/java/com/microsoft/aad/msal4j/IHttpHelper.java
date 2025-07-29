// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing the HTTP helper component that handles network communications.
 * <p>
 * This interface abstracts the HTTP communication layer used for sending requests.
 * It's used internally by the library to execute HTTP requests during various operations.
 */
interface IHttpHelper {

    /**
     * Executes an HTTP request.
     * <p>
     * This method handles all aspects of sending the HTTP request and processing the response,
     * such as applying retry policies and handling errors.
     *
     * @param httpRequest The HTTP request to be executed
     * @param requestContext Context information about the current request, including correlation IDs for telemetry
     * @param serviceBundle Bundle of services that may be needed during request execution, such as retry policies
     * @return An {@link IHttpResponse} object containing the response
     */
    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                     RequestContext requestContext,
                                     ServiceBundle serviceBundle);
}
