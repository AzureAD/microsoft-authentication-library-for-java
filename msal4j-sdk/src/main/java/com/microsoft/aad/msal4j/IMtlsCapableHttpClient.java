// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Marker contract for custom HTTP clients that honor request-specific mTLS settings.
 *
 * <p>Implementations must use {@link HttpRequest#sslSocketFactory()} when it is present
 * and must not automatically follow redirects for that credential-bound request.</p>
 */
public interface IMtlsCapableHttpClient extends IHttpClient {
}
