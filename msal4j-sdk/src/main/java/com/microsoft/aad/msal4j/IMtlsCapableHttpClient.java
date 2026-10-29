// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Marker contract for custom HTTP clients that honor request-specific mTLS settings.
 *
 * <p>Implementations must use {@link HttpRequest#sslContext()} or
 * {@link HttpRequest#sslSocketFactory()} when present and must not automatically
 * follow redirects for that credential-bound request.</p>
 *
 * <p>Engine-based and asynchronous transports should prefer the
 * {@link javax.net.ssl.SSLContext}. Socket-based JSSE transports can use the
 * {@link javax.net.ssl.SSLSocketFactory}.</p>
 */
public interface IMtlsCapableHttpClient extends IHttpClient {
}
