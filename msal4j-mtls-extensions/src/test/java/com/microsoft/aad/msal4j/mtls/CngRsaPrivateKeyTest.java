// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class CngRsaPrivateKeyTest {

    @Test
    void keyMaterialIsNotExportable() {
        CngRsaPrivateKey key = new CngRsaPrivateKey(
                Pointer.createConstant(42),
                BigInteger.valueOf(17),
                65537,
                () -> { });

        assertNull(key.getEncoded());
        assertNull(key.getFormat());
        assertEquals("RSA", key.getAlgorithm());
        assertFalse(key.toString().contains("42"));
    }

    @Test
    void closeReleasesNativeHandleExactlyOnce() {
        AtomicInteger releases = new AtomicInteger();
        CngRsaPrivateKey key = new CngRsaPrivateKey(
                Pointer.createConstant(42),
                BigInteger.valueOf(17),
                65537,
                releases::incrementAndGet);

        key.close();
        key.close();

        assertEquals(1, releases.get());
        assertTrue(key.isClosed());
    }

    @Test
    void closeWaitsForInFlightNativeOperation() throws Exception {
        AtomicInteger releases = new AtomicInteger();
        CountDownLatch operationStarted = new CountDownLatch(1);
        CountDownLatch finishOperation = new CountDownLatch(1);
        AtomicReference<Throwable> operationFailure = new AtomicReference<>();
        CngRsaPrivateKey key = new CngRsaPrivateKey(
                Pointer.createConstant(42),
                BigInteger.valueOf(17),
                65537,
                releases::incrementAndGet);

        Thread operation = new Thread(() -> {
            try {
                key.useNativeHandle(handle -> {
                    operationStarted.countDown();
                    try {
                        assertTrue(finishOperation.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            } catch (Throwable throwable) {
                operationFailure.set(throwable);
            }
        });
        operation.start();
        assertTrue(operationStarted.await(5, TimeUnit.SECONDS));

        Thread close = new Thread(key::close);
        close.start();
        close.join(100);
        assertTrue(close.isAlive());
        assertEquals(0, releases.get());

        finishOperation.countDown();
        operation.join(5000);
        close.join(5000);

        assertFalse(operation.isAlive());
        assertFalse(close.isAlive());
        assertNull(operationFailure.get());
        assertEquals(1, releases.get());
        assertTrue(key.isClosed());
    }
}
