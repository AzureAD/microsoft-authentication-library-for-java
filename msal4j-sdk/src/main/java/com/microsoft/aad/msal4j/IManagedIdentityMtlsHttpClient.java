// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * MSAL-owned HTTP callback used by the optional mTLS provider for IMDS v2 requests.
 */
public interface IManagedIdentityMtlsHttpClient {

    ManagedIdentityMtlsHttpResponse execute(ManagedIdentityMtlsHttpRequest request);
}
