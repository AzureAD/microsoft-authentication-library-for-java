// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.ManagedIdentityParameters;

/**
 * Public entry point for enabling KeyGuard attestation in managed identity
 * mTLS Proof-of-Possession flows.
 */
public final class ManagedIdentityAttestationExtensions {

    private ManagedIdentityAttestationExtensions() {
    }

    /**
     * Enables fail-closed MAA attestation through the optional Windows extension.
     *
     * <p>Call this after enabling either mTLS Proof-of-Possession or bearer-over-mTLS
     * on the supplied builder.</p>
     *
     * @param builder managed identity parameter builder
     * @return the same builder for continued configuration
     */
    public static ManagedIdentityParameters.ManagedIdentityParametersBuilder
    withAttestationSupport(
            ManagedIdentityParameters.ManagedIdentityParametersBuilder builder) {
        if (builder == null) {
            throw new NullPointerException("builder");
        }
        return builder.withManagedIdentityMtlsProvider(
                KeyGuardManagedIdentityMtlsProvider.withAttestationSupport());
    }
}
