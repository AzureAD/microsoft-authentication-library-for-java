// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.microsoft.aad.msal4j.ManagedIdentityMtlsBinding;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsHttpResponse;
import com.microsoft.aad.msal4j.ManagedIdentityMtlsRequest;
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
    void capabilityProbeAndBindingCreationShareTheSameKeyLock() throws Exception {
        KeyGuardManagedIdentityMtlsProvider provider =
                new KeyGuardManagedIdentityMtlsProvider();
        CountDownLatch probeEntered = new CountDownLatch(1);
        CountDownLatch releaseProbe = new CountDownLatch(1);
        CountDownLatch acquisitionStarted = new CountDownLatch(1);
        AtomicInteger httpCalls = new AtomicInteger();
        ManagedIdentityMtlsRequest request = new ManagedIdentityMtlsRequest(
                null, null, UUID.randomUUID().toString(), "correlation",
                ignored -> {
                    if (httpCalls.incrementAndGet() == 1) {
                        probeEntered.countDown();
                        try {
                            if (!releaseProbe.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("Timed out releasing capability probe.");
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                    }
                    throw new MtlsMsiException("Stop before accessing native keys.");
                }, false);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> probe = executor.submit(
                    () -> provider.getMaxSupportedBindingStrength(request));
            assertTrue(probeEntered.await(5, TimeUnit.SECONDS));
            Future<?> acquisition = executor.submit(() -> {
                acquisitionStarted.countDown();
                return provider.getOrCreateBinding(request);
            });
            assertTrue(acquisitionStarted.await(5, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class,
                    () -> acquisition.get(250, TimeUnit.MILLISECONDS));
            assertEquals(1, httpCalls.get());

            releaseProbe.countDown();

            assertInstanceOf(MtlsMsiException.class,
                    assertThrows(ExecutionException.class,
                            () -> probe.get(5, TimeUnit.SECONDS)).getCause());
            assertInstanceOf(MtlsMsiException.class,
                    assertThrows(ExecutionException.class,
                            () -> acquisition.get(5, TimeUnit.SECONDS)).getCause());
            assertEquals(2, httpCalls.get());
        } finally {
            releaseProbe.countDown();
            executor.shutdownNow();
        }
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

    @Test
    void providerSupportsBindingInvalidationForBoundedRetry() throws Exception {
        KeyGuardManagedIdentityMtlsProvider provider =
                new KeyGuardManagedIdentityMtlsProvider();
        ManagedIdentityMtlsRequest request = new ManagedIdentityMtlsRequest(
                null,
                null,
                "missing-binding",
                "correlation",
                ignored -> new ManagedIdentityMtlsHttpResponse(
                        500,
                        "",
                        java.util.Collections.emptyMap()),
                false);

        assertTrue(provider.invalidateBinding(
                request,
                generation(
                        Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                        new AtomicInteger()).binding));
    }

    @Test
    void unexpiredRejectedGenerationsRemainUsable() throws Exception {
        AtomicInteger oldestReleases = new AtomicInteger();
        AtomicInteger middleReleases = new AtomicInteger();
        AtomicInteger newestReleases = new AtomicInteger();
        List<KeyGuardManagedIdentityMtlsProvider.BindingGeneration> generations =
                new java.util.ArrayList<>(Arrays.asList(
                        generation(
                                Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                                oldestReleases),
                        generation(
                                Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                                middleReleases),
                        generation(
                                Instant.now().plus(Duration.ofHours(48)).toEpochMilli(),
                                newestReleases)));

        List<KeyGuardManagedIdentityMtlsProvider.BindingGeneration> retained =
                KeyGuardManagedIdentityMtlsProvider.cleanupRetiredGenerations(
                        generations,
                        Instant.now().toEpochMilli());

        assertEquals(3, retained.size());
        assertEquals(0, oldestReleases.get());
        assertEquals(0, middleReleases.get());
        assertEquals(0, newestReleases.get());
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
