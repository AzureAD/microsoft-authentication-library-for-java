// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class KeyGuardManagedIdentityMtlsProviderTest {

    @Test
    void certificateRotatesInsideTwentyFourHourWindow() {
        long now = Instant.now().toEpochMilli();

        assertTrue(KeyGuardManagedIdentityMtlsProvider.isCertificateCurrent(
                now + Duration.ofHours(25).toMillis(), now));
        assertFalse(KeyGuardManagedIdentityMtlsProvider.isCertificateCurrent(
                now + Duration.ofHours(24).toMillis(), now));
        assertFalse(KeyGuardManagedIdentityMtlsProvider.isCertificateCurrent(
                now + Duration.ofHours(1).toMillis(), now));
    }

    @Test
    void retiredGenerationPreventsUnlockedCacheHit() {
        long now = Instant.now().toEpochMilli();
        KeyGuardManagedIdentityMtlsProvider.BindingGeneration current =
                new KeyGuardManagedIdentityMtlsProvider.BindingGeneration(
                        null,
                        null,
                        now + Duration.ofHours(25).toMillis());

        assertTrue(KeyGuardManagedIdentityMtlsProvider
                .canReturnWithoutCleanup(current, false));
        assertFalse(KeyGuardManagedIdentityMtlsProvider
                .canReturnWithoutCleanup(current, true));
    }
}
