// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.interfaces.RSAPrivateKey;

import org.easymock.EasyMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.annotations.Test;

@Test(groups = { "checkin" })
@PrepareForTest({ RSAPrivateKey.class })
public class AsymmetricKeyCredentialTest extends AbstractMsalTests {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "PrivateKey is null or empty")
    public void testNullKey() {
        AsymmetricKeyCredential.create((PrivateKey) null, null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class, expectedExceptionsMessageRegExp = "certificate key size must be at least 2048")
    public void testInvalidKeysize() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2047).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        AsymmetricKeyCredential.create(key, null);
    }

    @Test
    public void testGetClient() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2048).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        final AsymmetricKeyCredential kc = AsymmetricKeyCredential.create(key, null);
        assertNotNull(kc);
    }

    @Test
    public void testGetKey() {
        final RSAPrivateKey key = EasyMock.createMock(RSAPrivateKey.class);
        final BigInteger modulus = EasyMock.createMock(BigInteger.class);
        EasyMock.expect(modulus.bitLength()).andReturn(2048).times(1);
        EasyMock.expect(key.getModulus()).andReturn(modulus).times(1);
        EasyMock.replay(modulus, key);
        final AsymmetricKeyCredential kc = AsymmetricKeyCredential.create(key, null);
        assertNotNull(kc);
    }
}
