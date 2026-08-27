// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Optional platform extension for attested KeyGuard managed identity mTLS PoP bindings.
 *
 * <p>Implementations are discovered with {@link java.util.ServiceLoader}. MSAL core remains
 * loadable when the optional Windows extension is absent.</p>
 */
public interface IManagedIdentityMtlsProvider {

    ManagedIdentityMtlsBinding getOrCreateBinding(ManagedIdentityMtlsRequest request);

    /**
     * Probes the strongest binding this provider can produce without acquiring a token.
     */
    default MtlsBindingStrength getMaxSupportedBindingStrength(
            ManagedIdentityMtlsRequest request) {
        return MtlsBindingStrength.NONE;
    }
}
