// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Assertions;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

//@Test//(groups = {"checkin"})
//@PrepareForTest({RSAPrivateKey.class})
public class ClientCertificateTest extends AbstractMsalTests {

    @Test
    public void testNullKey() {
        NullPointerException exception = Assertions.assertThrows(NullPointerException.class, () -> {
            ClientCertificate.create((PrivateKey) null, null);
        });

        assertEquals("PrivateKey is null or empty", exception.getMessage());
    }

    @Test
    public void testInvalidKeysize() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2047).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class, () -> {
            ClientCertificate.create(key, null);
        });

        assertEquals("certificate key size must be at least 2048", exception.getMessage());

    }

    @Test
    public void testGetClient() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2048).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        final ClientCertificate kc = ClientCertificate.create(key, null);
        assertNotNull(kc);
    }

    @Test
    public void testGetKey() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2048).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        final ClientCertificate kc = ClientCertificate.create(key, null);
        assertNotNull(kc);
    }
}
