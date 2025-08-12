// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP response returned after executing a {@link HttpRequest} through an {@link IHttpClient} implementation.
 * This interface encapsulates all the data returned by the authentication service or other HTTP endpoints
 * contacted during the authentication flows.
 * <p>
 * Custom implementations of {@link IHttpClient} must convert their native HTTP response objects to this interface.
 */
public interface IHttpResponse {

    /**
     * Gets the HTTP response status code.
     *
     * @return HTTP response status code. Common values include 200 (OK), 400 (Bad Request),
     *         401 (Unauthorized), and 500 (Internal Server Error).
     */
    int statusCode();

    /**
     * Gets the HTTP response headers.
     *
     * @return A map of HTTP header names to their values. If a header appears multiple times,
     *         its values are collected in a list. Header names are typically case-insensitive
     *         as per HTTP specification.
     */
    Map<String, List<String>> headers();

    /**
     * Gets the HTTP response body.
     *
     * @return The body of the HTTP response as a string. For responses from the Microsoft identity platform,
     *         this is typically JSON content containing tokens or error information.
     */
    String body();
}
