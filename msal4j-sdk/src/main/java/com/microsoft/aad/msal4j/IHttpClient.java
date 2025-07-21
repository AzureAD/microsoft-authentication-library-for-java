// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface to be implemented when configuring a custom HTTP client for {@link IPublicClientApplication} or
 * {@link IConfidentialClientApplication}.
 * <p>
 * MSAL Java uses HTTP to communicate with the Microsoft identity platform for authentication and token acquisition.
 * By default, MSAL uses the JDK's HttpURLConnection, but this interface allows applications to provide
 * their own HTTP client implementation. This is useful when:
 * <ul>
 *   <li>Applications need to customize network behavior (e.g., configure proxies, add custom headers)</li>
 *   <li>Applications want to use a specific HTTP client library like Apache HttpClient or OkHttp</li>
 *   <li>Applications need to integrate with existing network infrastructure or logging systems</li>
 * </ul>
 * <p>
 * For more details, see https://aka.ms/msal4j-http-client
 */
public interface IHttpClient {

    /**
     * Executes an outgoing HTTP request using the HTTP client implementation of choice.
     * <p>
     * This method should handle all the details of making the HTTP request and adapting
     * the response to the {@link IHttpResponse} interface. The implementation should also
     * include appropriate error handling for network-related issues.
     *
     * @param httpRequest The {@link HttpRequest} containing details of the request to execute,
     *                    including URL, headers, HTTP method, and body.
     * @return An {@link IHttpResponse} object containing the status code, headers, and body of the response.
     * @throws Exception Non-recoverable exceptions that cannot be handled by the HTTP client implementation.
     *                   Note that recoverable errors (e.g., retryable HTTP status codes, transient
     *                   network issues) should be handled by the implementation itself.
     */
    IHttpResponse send(HttpRequest httpRequest) throws Exception;
}
