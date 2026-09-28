// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SniMtlsPopE2ETest {

    @Test
    void certificateSelectionContinuesAfterInaccessiblePrivateKey()
            throws Exception {
        KeyStore store = mock(KeyStore.class);
        X509Certificate certificate = TestHelper.getX509Cert();
        String simpleName = SniMtlsPopE2E.simpleName(certificate);
        when(store.aliases()).thenReturn(Collections.enumeration(
                Arrays.asList("inaccessible", "accessible")));
        when(store.getCertificate("inaccessible")).thenReturn(certificate);
        when(store.getCertificate("accessible")).thenReturn(certificate);
        when(store.getKey("inaccessible", null))
                .thenThrow(new UnrecoverableKeyException("access denied"));
        when(store.getKey("accessible", null))
                .thenReturn(TestHelper.getPrivateKey());
        when(store.getCertificateChain("accessible")).thenReturn(
                new Certificate[]{certificate});

        SniMtlsPopE2E.CertificateCredential credential =
                SniMtlsPopE2E.selectCertificate(
                        store,
                        simpleName,
                        validDate(certificate));

        assertSame(TestHelper.getPrivateKey(), credential.privateKey);
        assertSame(certificate, credential.leaf);
    }

    @Test
    void certificateSelectionPreservesAllPrivateKeyFailures()
            throws Exception {
        KeyStore store = mock(KeyStore.class);
        X509Certificate certificate = TestHelper.getX509Cert();
        String simpleName = SniMtlsPopE2E.simpleName(certificate);
        when(store.aliases()).thenReturn(Collections.enumeration(
                Arrays.asList("first", "second")));
        when(store.getCertificate("first")).thenReturn(certificate);
        when(store.getCertificate("second")).thenReturn(certificate);
        when(store.getKey("first", null))
                .thenThrow(new UnrecoverableKeyException("first denied"));
        when(store.getKey("second", null))
                .thenThrow(new UnrecoverableKeyException("second denied"));

        AssertionError failure = assertThrows(
                AssertionError.class,
                () -> SniMtlsPopE2E.selectCertificate(
                        store,
                        simpleName,
                        validDate(certificate)));

        assertTrue(failure.getMessage().contains("2 matching alias(es)"));
        assertEquals(2, failure.getSuppressed().length);
        assertTrue(failure.getSuppressed()[0].getMessage().contains("first"));
        assertTrue(failure.getSuppressed()[1].getMessage().contains("second"));
    }

    @Test
    void tokenAcquisitionTimeoutCancelsFutureAndPreservesCause() {
        CompletableFuture<IAuthenticationResult> acquisition =
                new CompletableFuture<>();

        AssertionError failure = assertThrows(
                AssertionError.class,
                () -> SniMtlsPopE2E.awaitAcquisition(
                        acquisition, 1, TimeUnit.MILLISECONDS));

        assertTrue(acquisition.isCancelled());
        assertTrue(failure.getCause() instanceof TimeoutException);
        assertTrue(failure.getMessage().contains("timed out"));
    }

    private static Date validDate(X509Certificate certificate) {
        return new Date(
                certificate.getNotBefore().getTime()
                        + (certificate.getNotAfter().getTime()
                        - certificate.getNotBefore().getTime()) / 2);
    }
}
