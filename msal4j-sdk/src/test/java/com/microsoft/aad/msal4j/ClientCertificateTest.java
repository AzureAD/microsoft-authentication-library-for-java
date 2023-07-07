// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCertificateTest extends AbstractMsalTests {

    @Test
    void testNullKey() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ClientCertificate.create((PrivateKey) null, null));

        assertEquals("PrivateKey is null or empty", ex.getMessage());
    }

    @Test
    void testInvalidKeysize() {
        final RSAPrivateKey key = mock(RSAPrivateKey.class);
        final BigInteger modulus = mock(BigInteger.class);
        doReturn(2047).when(modulus).bitLength();
        doReturn(modulus).when(key).getModulus();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> ClientCertificate.create(key, null));

        assertEquals("certificate key size must be at least 2048", ex.getMessage());
    }

    @Test
    void testGetClient() {
        final RSAPrivateKey key = mock(RSAPrivateKey.class);
        final BigInteger modulus = mock(BigInteger.class);
        doReturn(2048).when(modulus).bitLength();
        doReturn(modulus).when(key).getModulus();

        final ClientCertificate kc = ClientCertificate.create(key, null);
        assertNotNull(kc);
    }
}
