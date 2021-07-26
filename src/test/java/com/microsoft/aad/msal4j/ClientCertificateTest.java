// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

import org.easymock.EasyMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.Test;

@Test(groups = {"checkin"})
@PrepareForTest({RSAPrivateKey.class})
public class ClientCertificateTest extends AbstractMsalTests {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "PrivateKey is null or empty")
    public void testNullKey() {
        ClientCertificate.create((PrivateKey) null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "certificate key size must be at least 2048")
    public void testInvalidKeysize() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2047).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        ClientCertificate.create(key, null);
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
