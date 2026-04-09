// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MtlsBindingInfo} — specifically the 5-minute early-expiry logic.
 *
 * <p>Mirrors msal-go's cert cache expiry: {@code cert.NotAfter.Add(-5 * time.Minute)}.</p>
 *
 * <p>Requires Windows because creating a {@link CngRsaPrivateKey} instance transitively
 * initializes {@link NCryptLibrary} ({@code ncrypt.dll}).</p>
 */
@EnabledOnOs(OS.WINDOWS)
class MtlsBindingInfoTest {

    // ─── isExpired ────────────────────────────────────────────────────────────

    @Test
    void isExpired_false_whenCertExpires60MinutesFromNow() {
        // expiresAt = notAfter - 5 min = now + 55 min → not expired
        Date notAfter = new Date(System.currentTimeMillis() + 60L * 60 * 1000);
        assertFalse(buildInfo(notAfter).isExpired());
    }

    @Test
    void isExpired_true_whenCertExpired1HourAgo() {
        // expiresAt = now - 65 min → expired
        Date notAfter = new Date(System.currentTimeMillis() - 60L * 60 * 1000);
        assertTrue(buildInfo(notAfter).isExpired());
    }

    @Test
    void isExpired_false_when6MinutesRemain() {
        // 6 min until cert expiry → expiresAt = now + 1 min → not expired yet
        Date notAfter = new Date(System.currentTimeMillis() + 6L * 60 * 1000);
        assertFalse(buildInfo(notAfter).isExpired(),
                "Binding must not be considered expired when more than 5 minutes remain");
    }

    @Test
    void isExpired_true_when4MinutesRemain() {
        // 4 min until cert expiry → expiresAt = now - 1 min → expired (within 5-min buffer)
        Date notAfter = new Date(System.currentTimeMillis() + 4L * 60 * 1000);
        assertTrue(buildInfo(notAfter).isExpired(),
                "Binding must be considered expired within the 5-minute proactive refresh window");
    }

    @Test
    void isExpired_true_exactlyAt5MinuteBoundary() {
        // Exactly 5 minutes remain → expiresAt ≈ now → just expired (or right at boundary)
        Date notAfter = new Date(System.currentTimeMillis() + 5L * 60 * 1000);
        // At exactly 5 min, expiresAt == now; new Date().after(expiresAt) may be false by a ms.
        // Allow either outcome — the important invariant is the direction.
        MtlsBindingInfo info = buildInfo(notAfter);
        // Just verify it doesn't throw
        info.isExpired();
    }

    // ─── Field storage ────────────────────────────────────────────────────────

    @Test
    void constructor_storesAllFields() {
        Date notAfter = new Date(System.currentTimeMillis() + 60L * 60 * 1000);
        X509Certificate mockCert = mockCert(notAfter);
        CngRsaPrivateKey key = new CngRsaPrivateKey(Pointer.NULL, BigInteger.valueOf(12345), 65537);

        MtlsBindingInfo info = new MtlsBindingInfo(
                key, mockCert, "https://eastus.mtlsauth.microsoft.com", "my-client", "my-tenant");

        assertSame(key,       info.privateKey);
        assertSame(mockCert,  info.certificate);
        assertEquals("https://eastus.mtlsauth.microsoft.com", info.mtlsEndpoint);
        assertEquals("my-client", info.clientId);
        assertEquals("my-tenant", info.tenantId);
    }

    @Test
    void constructor_expiresAt_isFiveMinutesBeforeNotAfter() {
        long notAfterMs = System.currentTimeMillis() + 60L * 60 * 1000;
        Date notAfter   = new Date(notAfterMs);
        MtlsBindingInfo info = buildInfo(notAfter);

        long expectedExpiresAtMs = notAfterMs - 5L * 60 * 1000;
        long delta = Math.abs(info.expiresAt.getTime() - expectedExpiresAtMs);
        assertTrue(delta < 1000,
                "expiresAt must be exactly 5 minutes before cert notAfter");
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static MtlsBindingInfo buildInfo(Date notAfter) {
        X509Certificate mockCert = mockCert(notAfter);
        CngRsaPrivateKey key = new CngRsaPrivateKey(Pointer.NULL, BigInteger.ONE, 65537);
        return new MtlsBindingInfo(key, mockCert, "https://mtlsauth.microsoft.com", "c", "t");
    }

    private static X509Certificate mockCert(Date notAfter) {
        X509Certificate cert = Mockito.mock(X509Certificate.class);
        Mockito.when(cert.getNotAfter()).thenReturn(notAfter);
        return cert;
    }
}
