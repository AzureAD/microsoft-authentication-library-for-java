// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ManagedIdentityMtlsProviderLoaderTest {

    @Test
    void missingOptionalProviderFailsClosed() {
        MsalClientException exception = assertThrows(
                MsalClientException.class,
                ManagedIdentityMtlsProviderLoader::load);

        assertEquals(MsalError.MANAGED_IDENTITY_MTLS_PROVIDER_UNAVAILABLE,
                exception.errorCode());
    }
}
