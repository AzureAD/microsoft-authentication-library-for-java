// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

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
}
