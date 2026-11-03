// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.microsoft.aad.msal4j.MtlsBindingStrength;
import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;

import javax.net.ssl.X509ExtendedKeyManager;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KeyGuardMtlsBindingContextTest {

    @Test
    void keyIdUsesFullLeafCertificateDer() throws Exception {
        byte[] certificateDer = new byte[]{1, 2, 3, 4, 5};
        X509Certificate certificate = mock(X509Certificate.class);
        when(certificate.getEncoded()).thenReturn(certificateDer);

        KeyGuardMtlsBindingContext context =
                new KeyGuardMtlsBindingContext(key(), certificate);
        String expected = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(certificateDer));

        assertEquals(expected, context.keyId());
        assertEquals(expected, context.keyId());
        assertNotNull(context.sslContext());
        assertSame(context.sslContext(), context.sslContext());
        assertEquals("TLSv1.2", context.sslContext().getProtocol());
        assertArrayEquals(
                new String[]{"TLSv1.2"},
                context.sslContext().getDefaultSSLParameters().getProtocols());
        assertNotNull(context.keyManager());
        assertSame(context.keyManager(), context.keyManager());
        assertSame(certificate, context.bindingCertificate());
        assertEquals(MtlsBindingStrength.KEY_GUARD,
                context.bindingStrength());
    }

    @Test
    void rotatingTlsFollowsNewGenerationWhileOldTokenBindingStaysFixed() throws Exception {
        X509Certificate firstCertificate = mock(X509Certificate.class);
        X509Certificate secondCertificate = mock(X509Certificate.class);
        when(firstCertificate.getEncoded()).thenReturn(new byte[]{1});
        when(secondCertificate.getEncoded()).thenReturn(new byte[]{2});
        CngRsaPrivateKey firstKey = key();
        CngRsaPrivateKey secondKey = key();
        KeyGuardMtlsBindingContext.RotatingTlsContext rotating =
                new KeyGuardMtlsBindingContext.RotatingTlsContext();
        KeyGuardMtlsBindingContext first =
                new KeyGuardMtlsBindingContext(firstKey, firstCertificate, rotating);
        KeyGuardMtlsBindingContext second =
                new KeyGuardMtlsBindingContext(secondKey, secondCertificate, rotating);

        first.activate();
        X509ExtendedKeyManager manager = first.rotatingKeyManager();
        String firstAlias = manager.chooseClientAlias(new String[]{"RSA"}, null, null);
        second.activate();
        String secondAlias = manager.chooseEngineClientAlias(new String[]{"RSA"}, null, null);

        assertSame(manager, second.rotatingKeyManager());
        assertSame(first.rotatingSslContext(), second.rotatingSslContext());
        assertEquals("TLSv1.2", first.rotatingSslContext().getProtocol());
        assertNotSame(first.sslContext(), second.sslContext());
        assertNotEquals(firstAlias, secondAlias);
        assertSame(secondKey, manager.getPrivateKey(secondAlias));
        assertArrayEquals(new X509Certificate[]{secondCertificate},
                manager.getCertificateChain(secondAlias));
        assertSame(firstKey, manager.getPrivateKey(firstAlias));
        assertArrayEquals(new X509Certificate[]{firstCertificate},
                manager.getCertificateChain(firstAlias));
        assertEquals(firstAlias,
                first.keyManager().chooseClientAlias(new String[]{"RSA"}, null, null));
        assertSame(firstCertificate, first.bindingCertificate());
        assertNotEquals(first.keyId(), second.keyId());

        first.closeNativeKey();

        assertNull(manager.getPrivateKey(firstAlias));
        assertNull(manager.getCertificateChain(firstAlias));
        assertSame(secondKey, manager.getPrivateKey(secondAlias));
        assertEquals(secondAlias, manager.chooseClientAlias(new String[]{"RSA"}, null, null));
        second.closeNativeKey();
    }

    @Test
    void renewedCertificateWithSameKeyChangesBindingKeyId() throws Exception {
        X509Certificate first = mock(X509Certificate.class);
        X509Certificate second = mock(X509Certificate.class);
        when(first.getEncoded()).thenReturn(new byte[]{1});
        when(second.getEncoded()).thenReturn(new byte[]{2});
        CngRsaPrivateKey key = key();

        assertNotEquals(
                new KeyGuardMtlsBindingContext(key, first).keyId(),
                new KeyGuardMtlsBindingContext(key, second).keyId());
    }

    private static CngRsaPrivateKey key() {
        return new CngRsaPrivateKey(
                Pointer.createConstant(42),
                BigInteger.valueOf(17),
                65537,
                () -> { });
    }
}
