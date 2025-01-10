// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.util.Collections;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCertificateTest {

    @Test
    void testNullKey() {
        NullPointerException ex = assertThrows(NullPointerException.class, () -> ClientCertificate.create((PrivateKey) null, null));

        assertEquals("PrivateKey is null or empty", ex.getMessage());
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

    @Test
    void testIClientCertificateInterface_Sha256AndSha1() throws NoSuchAlgorithmException, CertificateException {
        //See https://github.com/AzureAD/microsoft-authentication-library-for-java/issues/863 for context on this test.
        //Essentially, it aims to test compatibility for customers that implemented IClientCertificate in older versions of the library.

        //IClientCertificate.publicCertificateHash256() returns null by default if not implemented...
        IClientCertificate certificate = new TestClientCredential();
        assertNull(certificate.publicCertificateHash256());

        //... but ClientCredentialFactory has an implemented version, so it should not be null.
        certificate = ClientCredentialFactory.createFromCertificate(TestHelper.getPrivateKey(), TestHelper.getX509Cert());
        assertNotNull(certificate.publicCertificateHash256());
    }

    class TestClientCredential implements IClientCertificate {
        @Override
        public PrivateKey privateKey() {
            return null;
        }

        @Override
        public String publicCertificateHash() {
            return "";
        }

        @Override
        public List<String> getEncodedPublicKeyCertificateChain() {
            return Collections.emptyList();
        }
    }
}
