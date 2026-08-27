// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedIdentityCapabilitiesTest {

    @Test
    void keyGuardCapabilityIsReportedAsSupported() {
        ManagedIdentityCapabilities capabilities =
                new ManagedIdentityCapabilities(
                        ManagedIdentitySourceType.IMDS,
                        MtlsBindingStrength.KEY_GUARD,
                        null);

        assertEquals(ManagedIdentitySourceType.IMDS,
                capabilities.source());
        assertEquals(MtlsBindingStrength.KEY_GUARD,
                capabilities.maxSupportedBindingStrength());
        assertTrue(capabilities.isMtlsPopSupportedByHost());
        assertNull(capabilities.errorReason());
    }

    @Test
    void unavailableCapabilityIncludesReason() {
        ManagedIdentityCapabilities capabilities =
                new ManagedIdentityCapabilities(
                        ManagedIdentitySourceType.DEFAULT_TO_IMDS,
                        MtlsBindingStrength.NONE,
                        "IMDS v2 unavailable");

        assertFalse(capabilities.isMtlsPopSupportedByHost());
        assertEquals("IMDS v2 unavailable",
                capabilities.errorReason());
    }

    @Test
    void providerDefaultDoesNotOverclaimSupport() {
        IManagedIdentityMtlsProvider provider = request -> null;

        assertEquals(MtlsBindingStrength.NONE,
                provider.getMaxSupportedBindingStrength(null));
    }

    @Test
    void applicationRetriesUnavailableImdsDiscovery()
            throws Exception {
        ManagedIdentityApplication application = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .build();

        CompletableFuture<ManagedIdentityCapabilities> first =
                application.getManagedIdentityCapabilities();

        ManagedIdentityCapabilities capabilities = first.get();
        assertEquals(MtlsBindingStrength.NONE,
                capabilities.maxSupportedBindingStrength());
        assertFalse(capabilities.isMtlsPopSupportedByHost());
        assertTrue(capabilities.errorReason().contains(
                "msal4j-mtls-extensions"));

        CompletableFuture<ManagedIdentityCapabilities> retry =
                application.getManagedIdentityCapabilities();
        assertTrue(retry != first);
    }
}
