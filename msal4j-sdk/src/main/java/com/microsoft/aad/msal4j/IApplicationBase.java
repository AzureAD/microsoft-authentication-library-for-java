// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing an application for which tokens can be acquired.
 */
interface IApplicationBase {

    String DEFAULT_AUTHORITY = "https://login.microsoftonline.com/common/";

    /**
     * @return a boolean value which determines whether Pii (personally identifiable information) will be logged in
     */
    boolean logPii();

    /**
     * @return Correlation ID which is used for diagnostics purposes, is attached to token service requests
     * Default value is random UUID
     */
    String correlationId();

    /**
     * Sets HTTP client to be used by the client application for all HTTP requests. Allows for fine-grained
     * configuration of HTTP client.

     * @return instance of IHttpClient used by the application
     */
    IHttpClient httpClient();

    /**
     * @return proxy used by the application for all network communication.
     */
    Proxy proxy();

    /**
     * @return SSLSocketFactory used by the application for all network communication.
     */
    SSLSocketFactory sslSocketFactory();
}
