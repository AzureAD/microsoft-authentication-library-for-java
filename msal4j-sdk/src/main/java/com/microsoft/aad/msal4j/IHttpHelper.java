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

    /**
     * Executes an HTTP request using an explicitly-provided HTTP client and telemetry manager rather than
     * the app-level {@link ServiceBundle} client. Used for requests that require a bespoke transport, such
     * as mTLS Proof-of-Possession where the client certificate must be presented on the TLS handshake.
     *
     * @param httpRequest The HTTP request to be executed
     * @param requestContext Context information about the current request, including correlation IDs for telemetry
     * @param telemetryManager The telemetry manager to use for this request
     * @param httpClient The HTTP client to send the request with
     * @return An {@link IHttpResponse} object containing the response
     */
    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                     RequestContext requestContext,
                                     TelemetryManager telemetryManager,
                                     IHttpClient httpClient);
}
