// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AttestationTokenCacheTest {

    @Test
    void cacheNormalizesEndpointAndIsScopedByKeyId() throws Exception {
        AttestationTokenCache cache = new AttestationTokenCache();
        AtomicInteger loads = new AtomicInteger();

        String first = cache.getOrAttest("HTTPS://Example.COM/", "key-a",
                () -> token(loads.incrementAndGet(), 3600));
        String second = cache.getOrAttest("https://example.com", "key-a",
                () -> token(loads.incrementAndGet(), 3600));
        String otherKey = cache.getOrAttest("https://example.com", "key-b",
                () -> token(loads.incrementAndGet(), 3600));

        assertEquals(first, second);
        assertNotEquals(first, otherKey);
        assertEquals(2, loads.get());
    }

    @Test
    void staleTokenInsideFreshnessBufferIsNotReused() throws Exception {
        AttestationTokenCache cache = new AttestationTokenCache();
        AtomicInteger loads = new AtomicInteger();

        assertThrows(MtlsMsiException.class,
                () -> cache.getOrAttest("https://example.com", "key",
                        () -> token(loads.incrementAndGet(), 299)));
        cache.getOrAttest("https://example.com", "key",
                () -> token(loads.incrementAndGet(), 3600));

        assertEquals(2, loads.get());
    }

    @Test
    void concurrentCallsAreSingleFlightPerKey() throws Exception {
        AttestationTokenCache cache = new AttestationTokenCache();
        AtomicInteger loads = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            Callable<String> task = () -> cache.getOrAttest(
                    "https://example.com",
                    "key",
                    () -> {
                        loads.incrementAndGet();
                        try {
                            Thread.sleep(25);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException(e);
                        }
                        return token(1, 3600);
                    });
            List<Future<String>> futures = executor.invokeAll(
                    Collections.nCopies(8, task));
            for (Future<String> future : futures) {
                assertEquals(futures.get(0).get(), future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, loads.get());
    }

    @Test
    void failedLoadIsNotCached() {
        AttestationTokenCache cache = new AttestationTokenCache();
        AtomicInteger attempts = new AtomicInteger();

        assertThrows(Exception.class, () -> cache.getOrAttest(
                "https://example.com", "key", () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("attestation failed");
                }));
        assertThrows(Exception.class, () -> cache.getOrAttest(
                "https://example.com", "key", () -> {
                    attempts.incrementAndGet();
                    throw new IllegalStateException("attestation failed");
                }));

        assertEquals(2, attempts.get());
    }

    private static String token(int marker, long validForSeconds) {
        String header = encode("{\"alg\":\"none\"}");
        String payload = encode("{\"marker\":" + marker + ",\"exp\":"
                + (Instant.now().getEpochSecond() + validForSeconds) + "}");
        return header + "." + payload + ".signature";
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }
}
