// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Request passed from MSAL core to the optional KeyGuard mTLS provider.
 */
public final class ManagedIdentityMtlsRequest {

    private final String identityQueryParameter;
    private final String identityQueryValue;
    private final String bindingCacheKey;
    private final String correlationId;
    private final IManagedIdentityMtlsHttpClient httpClient;
    private final boolean attestationEnabled;
    private final boolean forceRefresh;

    public ManagedIdentityMtlsRequest(
            String identityQueryParameter,
            String identityQueryValue,
            String bindingCacheKey,
            String correlationId,
            IManagedIdentityMtlsHttpClient httpClient) {
        this(identityQueryParameter, identityQueryValue, bindingCacheKey,
                correlationId, httpClient, true);
    }

    public ManagedIdentityMtlsRequest(
            String identityQueryParameter,
            String identityQueryValue,
            String bindingCacheKey,
            String correlationId,
            IManagedIdentityMtlsHttpClient httpClient,
            boolean attestationEnabled) {
        this(identityQueryParameter, identityQueryValue, bindingCacheKey,
                correlationId, httpClient, attestationEnabled, false);
    }

    public ManagedIdentityMtlsRequest(
            String identityQueryParameter,
            String identityQueryValue,
            String bindingCacheKey,
            String correlationId,
            IManagedIdentityMtlsHttpClient httpClient,
            boolean attestationEnabled,
            boolean forceRefresh) {
        this.identityQueryParameter = identityQueryParameter;
        this.identityQueryValue = identityQueryValue;
        this.bindingCacheKey = bindingCacheKey;
        this.correlationId = correlationId;
        this.httpClient = httpClient;
        this.attestationEnabled = attestationEnabled;
        this.forceRefresh = forceRefresh;
    }

    public String identityQueryParameter() {
        return identityQueryParameter;
    }

    public String identityQueryValue() {
        return identityQueryValue;
    }

    public String bindingCacheKey() {
        return bindingCacheKey;
    }

    public String correlationId() {
        return correlationId;
    }

    public IManagedIdentityMtlsHttpClient httpClient() {
        return httpClient;
    }

    public boolean attestationEnabled() {
        return attestationEnabled;
    }

    /**
     * Requires a newly issued binding certificate instead of a cached binding.
     * Providers may reuse the current key and a still-valid attestation token.
     */
    public boolean forceRefresh() {
        return forceRefresh;
    }
}
