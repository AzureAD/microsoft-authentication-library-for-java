// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.aad.msal4j.ManagedIdentityMtlsBinding;
import com.sun.jna.Pointer;

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void cleanupClosesOnlyExpiredRetiredGenerations() throws Exception {
        long now = Instant.now().toEpochMilli();
        AtomicInteger expiredReleases = new AtomicInteger();
        AtomicInteger activeReleases = new AtomicInteger();
        KeyGuardManagedIdentityMtlsProvider.BindingGeneration expired =
                generation(now - 1, expiredReleases);
        KeyGuardManagedIdentityMtlsProvider.BindingGeneration active =
                generation(now + Duration.ofHours(1).toMillis(), activeReleases);

        List<KeyGuardManagedIdentityMtlsProvider.BindingGeneration> retained =
                KeyGuardManagedIdentityMtlsProvider.cleanupRetiredGenerations(
                        Arrays.asList(expired, active),
                        now);

        assertEquals(1, retained.size());
        assertSame(active, retained.get(0));
        assertEquals(1, expiredReleases.get());
        assertEquals(0, activeReleases.get());
    }

    private static KeyGuardManagedIdentityMtlsProvider.BindingGeneration generation(
            long notAfterMillis,
            AtomicInteger releases) throws Exception {
        CngRsaPrivateKey key = new CngRsaPrivateKey(
                Pointer.createConstant(notAfterMillis),
                BigInteger.valueOf(17),
                65537,
                releases::incrementAndGet);
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getEncoded()).thenReturn(
                Long.toString(notAfterMillis).getBytes("UTF-8"));
        KeyGuardMtlsBindingContext context =
                new KeyGuardMtlsBindingContext(key, certificate);
        return new KeyGuardManagedIdentityMtlsProvider.BindingGeneration(
                new ManagedIdentityMtlsBinding(
                        context,
                        "client",
                        "https://login.example/token"),
                context,
                notAfterMillis);
    }
}
