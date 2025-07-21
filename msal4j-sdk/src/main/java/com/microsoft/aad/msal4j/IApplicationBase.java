// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * Base interface representing a client application that can acquire tokens from the Microsoft identity platform.
 * Defines common functionality across different application types (public client, confidential client,
 * and managed identity applications).
 */
interface IApplicationBase {

    String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    /**
     * Gets whether personally identifiable information (PII) is included in log messages.
     *
     * @return A boolean value which determines whether PII (personally identifiable information) will be included in log messages.
     * When true, PII will be logged; when false, PII will be masked or omitted from logs.
     */
    boolean logPii();

    /**
     * Gets the correlation ID used for tracing requests through the authentication system.
     *
     * @return Correlation ID which is used for diagnostics purposes and is attached to token service requests.
     * The default value is a random UUID.
     */
    String correlationId();

    /**
     * Gets the HTTP client used by the application for all HTTP requests.
     *
     * @return Instance of IHttpClient used by the application for network communication with the Microsoft identity platform.
     */
    IHttpClient httpClient();

    /**
     * Gets the proxy configuration used by the application for network communication.
     *
     * @return Proxy used by the application for all network communication. Returns null if no proxy is configured.
     */
    Proxy proxy();

    /**
     * Gets the SSL socket factory used by the application for secure network communication.
     *
     * @return SSLSocketFactory used by the application for all secure network communication. Returns null if no custom SSL socket factory is configured.
     */
    SSLSocketFactory sslSocketFactory();
}
