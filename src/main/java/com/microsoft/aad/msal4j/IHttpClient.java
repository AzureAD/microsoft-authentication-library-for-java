// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface to be implemented when configuring http client for {@link IPublicClientApplication} or
 * {@link IConfidentialClientApplication}.
 */
public interface IHttpClient {

    /**
     *  Should implement execution of outgoing HTTP request with HTTP client of choice. Adapts
     *  response returned from HTTP client to {@link IHttpResponse}
     * @param httpRequest {@link HttpRequest}
     * @return {@link IHttpResponse}.
     * @throws Exception Non-recoverable exception. Recoverable exceptions should be handled by the
     * IHttpClient implementation
     */
    IHttpResponse send(HttpRequest httpRequest) throws Exception;
}
