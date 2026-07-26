// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing a link in the HTTP request chain (throttling, telemetry, correlation-id
 * verification, retry, sending).
 * <p>
 * Each link in the chain implements this interface and delegates to its successor, referenced only
 * through this interface, so that the request/response flows through all of them in sequence.
 */
interface IRequestChain {

    /**
     * Executes an HTTP request.
     *
     * @param httpRequest The HTTP request to be executed
     * @param requestContext Context information about the current request, including correlation IDs for telemetry
     * @param serviceBundle Bundle of services that may be needed during request execution, such as retry policies
     * @return An {@link IHttpResponse} object containing the response
     * @throws Exception Implementations further down the chain (e.g. the actual send) may throw; implementations
     *                    that handle/wrap exceptions themselves (e.g. telemetry) do not declare this.
     */
    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                     RequestContext requestContext,
                                     ServiceBundle serviceBundle) throws Exception;
}
