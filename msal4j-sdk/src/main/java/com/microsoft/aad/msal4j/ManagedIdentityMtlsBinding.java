// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Immutable binding generation returned by the optional managed identity mTLS provider.
 */
public final class ManagedIdentityMtlsBinding {

    private final IMtlsBindingContext bindingContext;
    private final String clientId;
    private final String tokenEndpoint;

    public ManagedIdentityMtlsBinding(
            IMtlsBindingContext bindingContext,
            String clientId,
            String tokenEndpoint) {
        if (bindingContext == null) {
            throw new NullPointerException("bindingContext");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId must not be blank");
        }
        if (tokenEndpoint == null || tokenEndpoint.trim().isEmpty()) {
            throw new IllegalArgumentException("tokenEndpoint must not be blank");
        }
        try {
            URL endpoint = new URL(tokenEndpoint);
            if (!"https".equalsIgnoreCase(endpoint.getProtocol())) {
                throw new IllegalArgumentException(
                        "tokenEndpoint must use HTTPS");
            }
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(
                    "tokenEndpoint must be a valid HTTPS URL", e);
        }
        this.bindingContext = bindingContext;
        this.clientId = clientId;
        this.tokenEndpoint = tokenEndpoint;
    }

    public IMtlsBindingContext bindingContext() {
        return bindingContext;
    }

    public String clientId() {
        return clientId;
    }

    public String tokenEndpoint() {
        return tokenEndpoint;
    }
}
