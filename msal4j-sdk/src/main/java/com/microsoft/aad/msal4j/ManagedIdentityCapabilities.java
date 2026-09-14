// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Managed identity and mTLS PoP capabilities detected for the current host.
 */
public final class ManagedIdentityCapabilities {

    private final ManagedIdentitySourceType source;
    private final MtlsBindingStrength maximumBindingStrength;
    private final String errorReason;

    ManagedIdentityCapabilities(
            ManagedIdentitySourceType source,
            MtlsBindingStrength maximumBindingStrength,
            String errorReason) {
        this.source = source;
        this.maximumBindingStrength = maximumBindingStrength;
        this.errorReason = errorReason;
    }

    public ManagedIdentitySourceType source() {
        return source;
    }

    public MtlsBindingStrength maxSupportedBindingStrength() {
        return maximumBindingStrength;
    }

    public boolean isMtlsPopSupportedByHost() {
        return maximumBindingStrength != MtlsBindingStrength.NONE;
    }

    public String errorReason() {
        return errorReason;
    }
}
