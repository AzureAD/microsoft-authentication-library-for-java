// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedIdentityCapabilitiesTest {

    @AfterEach
    void resetEnvironment() {
        ManagedIdentityApplication.setEnvironmentVariables(null);
    }

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
                "msal4j-key-attestation"));

        CompletableFuture<ManagedIdentityCapabilities> retry =
                application.getManagedIdentityCapabilities();
        assertTrue(retry != first);
    }

    @Test
    void imdsV2KillSwitchAcceptsOnlyTrueOrOne() {
        assertKillSwitchValue("true", true);
        assertKillSwitchValue("TRUE", true);
        assertKillSwitchValue("1", true);
        assertKillSwitchValue(null, false);
        assertKillSwitchValue("", false);
        assertKillSwitchValue("false", false);
        assertKillSwitchValue("0", false);
        assertKillSwitchValue("yes", false);
    }

    @Test
    void killSwitchReportsNoBindingSupportWithoutProbingProvider()
            throws Exception {
        setKillSwitch("true");
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
                Constants.MSAL_MI_DISABLE_IMDS_V2));
        assertSame(first, application.getManagedIdentityCapabilities());
    }

    private static void assertKillSwitchValue(
            String value,
            boolean expected) {
        setKillSwitch(value);
        assertEquals(expected,
                ManagedIdentityEnvironment.isImdsV2Disabled());
    }

    private static void setKillSwitch(String value) {
        ManagedIdentityApplication.setEnvironmentVariables(
                name -> Constants.MSAL_MI_DISABLE_IMDS_V2.equals(name)
                        ? value : null);
    }
}
