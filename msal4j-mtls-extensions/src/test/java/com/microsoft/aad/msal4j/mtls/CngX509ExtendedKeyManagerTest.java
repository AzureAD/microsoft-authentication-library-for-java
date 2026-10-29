// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.security.auth.x500.X500Principal;
import java.math.BigInteger;
import java.security.Principal;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class CngX509ExtendedKeyManagerTest {

    @Test
    void selectsOnlyRsaClientAliasForSocketAndEngine() throws Exception {
        CngRsaPrivateKey key = new CngRsaPrivateKey(
                Pointer.createConstant(42),
                BigInteger.valueOf(17),
                65537,
                () -> { });
        X509Certificate certificate = mock(X509Certificate.class);
        CngX509ExtendedKeyManager manager =
                new CngX509ExtendedKeyManager(key, certificate);
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        String alias = manager.chooseClientAlias(new String[]{"RSA"}, null, null);

        assertNotNull(alias);
        assertEquals(alias,
                manager.chooseEngineClientAlias(new String[]{"EC", "RSA"}, null, engine));
        assertNull(manager.chooseClientAlias(new String[]{"EC"}, null, null));
        assertNull(manager.chooseEngineClientAlias(new String[]{"EC"}, null, engine));
        assertSame(key, manager.getPrivateKey(alias));
        assertArrayEquals(new X509Certificate[]{certificate},
                manager.getCertificateChain(alias));
    }

    @Test
    void clientAliasSelectionIsIndependentOfIssuerHints() throws Exception {
        CngX509ExtendedKeyManager manager =
                new CngX509ExtendedKeyManager(key(), mock(X509Certificate.class));
        SSLEngine engine = SSLContext.getDefault().createSSLEngine();
        Principal[] nonMatchingIssuers = new Principal[]{
                new X500Principal("CN=Unrelated Issuer")
        };

        String alias = manager.chooseClientAlias(
                new String[]{"RSA"},
                new Principal[0],
                null);

        assertNotNull(alias);
        assertEquals(alias, manager.chooseClientAlias(
                new String[]{"RSA"},
                nonMatchingIssuers,
                null));
        assertEquals(alias, manager.chooseEngineClientAlias(
                new String[]{"RSA"},
                new Principal[0],
                engine));
        assertEquals(alias, manager.chooseEngineClientAlias(
                new String[]{"RSA"},
                nonMatchingIssuers,
                engine));
    }

    private static CngRsaPrivateKey key() {
        return new CngRsaPrivateKey(
                Pointer.createConstant(42),
                BigInteger.valueOf(17),
                65537,
                () -> { });
    }
}
