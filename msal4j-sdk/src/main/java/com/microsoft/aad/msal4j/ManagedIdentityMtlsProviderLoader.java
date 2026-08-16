// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Iterator;
import java.util.ServiceLoader;

final class ManagedIdentityMtlsProviderLoader {

    private ManagedIdentityMtlsProviderLoader() {
    }

    static IManagedIdentityMtlsProvider load() {
        ServiceLoader<IManagedIdentityMtlsProvider> loader =
                ServiceLoader.load(IManagedIdentityMtlsProvider.class);
        Iterator<IManagedIdentityMtlsProvider> providers = loader.iterator();
        if (!providers.hasNext()) {
            throw new MsalClientException(
                    "Managed identity mTLS PoP requires the optional "
                            + "com.microsoft.azure:msal4j-mtls-extensions dependency.",
                    MsalError.MANAGED_IDENTITY_MTLS_PROVIDER_UNAVAILABLE);
        }

        IManagedIdentityMtlsProvider provider = providers.next();
        if (providers.hasNext()) {
            throw new MsalClientException(
                    "Multiple managed identity mTLS providers were found. Configure exactly one provider.",
                    MsalError.MANAGED_IDENTITY_MTLS_PROVIDER_UNAVAILABLE);
        }
        return provider;
    }
}
