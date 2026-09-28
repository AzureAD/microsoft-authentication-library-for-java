// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Marker contract for custom HTTP clients that honor request-specific mTLS settings.
 *
 * <p>Implementations must consume {@link HttpRequest#sslContext()} or
 * {@link HttpRequest#sslSocketFactory()} and must honor
 * {@link HttpRequest#followRedirects()}.</p>
 */
public interface IMtlsCapableHttpClient extends IHttpClient {
}
