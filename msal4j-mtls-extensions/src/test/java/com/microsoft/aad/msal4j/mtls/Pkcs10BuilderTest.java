// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.math.BigInteger;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.nullable;

/**
 * Unit tests for {@link Pkcs10Builder} DER encoding.
 *
 * <p>Tests are split into two groups:</p>
 * <ol>
 *   <li><strong>Pure DER primitives</strong> — no CNG required; run on all platforms.</li>
 *   <li><strong>Full CSR generation</strong> — requires Windows (loads NCryptLibrary via
 *       CngKeyGuard); {@link CngKeyGuard#signPss} is mocked so no real CNG key is needed.</li>
 * </ol>
 *
 * <p>The CSR format must match msal-go's {@code generateCSR()} and MSAL.NET's
 * {@code Csr.Generate()} exactly so that the Azure IMDS {@code /issuecredential}
 * endpoint can parse and validate it.</p>
 */
class Pkcs10BuilderTest {

    // ─── DER primitives (pure Java, cross-platform) ───────────────────────────

    @Test
    void derSequence_short_wrapsWithTag30() {
        byte[] content = {0x01, 0x02, 0x03};
        byte[] seq = Pkcs10Builder.derSequence(content);

        assertEquals(0x30, seq[0] & 0xFF, "SEQUENCE tag must be 0x30");
        assertEquals(3,    seq[1] & 0xFF, "Short-form length must equal content length");
        assertEquals(0x01, seq[2]);
        assertEquals(0x02, seq[3]);
        assertEquals(0x03, seq[4]);
        assertEquals(5, seq.length);
    }

    @Test
    void derSequence_shortFormMaxLength() {
        // 127 bytes is the maximum for single-byte short-form length
        byte[] content = new byte[127];
        byte[] seq = Pkcs10Builder.derSequence(content);

        assertEquals(0x30, seq[0] & 0xFF);
        assertEquals(127,  seq[1] & 0xFF);
        assertEquals(2 + 127, seq.length);
    }

    @Test
    void derSequence_longForm1Byte_length128() {
        // 128 bytes requires 0x81 long-form header
        byte[] content = new byte[128];
        byte[] seq = Pkcs10Builder.derSequence(content);

        assertEquals(0x30, seq[0] & 0xFF);
        assertEquals(0x81, seq[1] & 0xFF, "Long-form header byte for lengths 128-255 must be 0x81");
        assertEquals(128,  seq[2] & 0xFF);
        assertEquals(3 + 128, seq.length);
    }

    @Test
    void derSequence_longForm2Byte_length256() {
        // 256 bytes requires 0x82 two-byte length
        byte[] content = new byte[256];
        byte[] seq = Pkcs10Builder.derSequence(content);

        assertEquals(0x30, seq[0] & 0xFF);
        assertEquals(0x82, seq[1] & 0xFF, "Long-form header for lengths 256+ must be 0x82");
        assertEquals(1,    seq[2] & 0xFF, "High byte of length 256 (0x0100)");
        assertEquals(0,    seq[3] & 0xFF, "Low byte of length 256");
        assertEquals(4 + 256, seq.length);
    }

    @Test
    void derBitString_prependsZeroUnusedBitsByte() {
        byte[] data = {0x01, 0x02};
        byte[] bs = Pkcs10Builder.derBitString(data);

        assertEquals(0x03, bs[0] & 0xFF, "BIT STRING tag must be 0x03");
        assertEquals(3,    bs[1] & 0xFF, "Length must cover the unused-bits byte + data");
        assertEquals(0x00, bs[2],        "Unused bits must be 0x00 (byte-aligned content)");
        assertEquals(0x01, bs[3]);
        assertEquals(0x02, bs[4]);
    }

    @Test
    void derBitString_empty_hasOnlyUnusedBitsByte() {
        byte[] bs = Pkcs10Builder.derBitString(new byte[0]);
        assertEquals(0x03, bs[0] & 0xFF);
        assertEquals(1,    bs[1] & 0xFF);
        assertEquals(0x00, bs[2]);
    }

    // ─── Full CSR generation (Windows only — CngKeyGuard is mocked) ───────────

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void generate_outputIsBase64EncodedDerSequence() throws Exception {
        BigInteger modulus = BigInteger.valueOf(2).pow(2047).add(BigInteger.ONE);
        byte[] fakeSignature = new byte[256]; // 2048-bit RSA output size

        try (MockedStatic<CngKeyGuard> mockCng = Mockito.mockStatic(CngKeyGuard.class)) {
            mockCng.when(() -> CngKeyGuard.signPss(any(), any(), anyString(), anyInt()))
                   .thenReturn(fakeSignature);

            String b64 = Pkcs10Builder.generate(
                    Pointer.NULL, modulus, 65537,
                    "test-client-id", "test-tenant-id", "vm-id-1", null);

            assertNotNull(b64);
            byte[] der = Base64.getDecoder().decode(b64);
            assertEquals(0x30, der[0] & 0xFF,
                    "Outermost CSR element must be a DER SEQUENCE (0x30)");
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void generate_cuIdAttributeContainsVmIdAndVmssId() throws Exception {
        BigInteger modulus = BigInteger.valueOf(2).pow(2047).add(BigInteger.ONE);
        byte[] fakeSignature = new byte[256];

        try (MockedStatic<CngKeyGuard> mockCng = Mockito.mockStatic(CngKeyGuard.class)) {
            mockCng.when(() -> CngKeyGuard.signPss(any(), any(), anyString(), anyInt()))
                   .thenReturn(fakeSignature);

            String b64 = Pkcs10Builder.generate(
                    Pointer.NULL, modulus, 65537,
                    "client-a", "tenant-b", "my-vm-id", "my-vmss-id");

            byte[] der = Base64.getDecoder().decode(b64);
            String derText = new String(der, java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(derText.contains("my-vm-id"),
                    "CSR DER must embed vmId in the cuId attribute");
            assertTrue(derText.contains("my-vmss-id"),
                    "CSR DER must embed vmssId in the cuId attribute");
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void generate_cuIdAttributeEmptyObject_whenBothIdsNull() throws Exception {
        BigInteger modulus = BigInteger.valueOf(2).pow(2047).add(BigInteger.ONE);
        byte[] fakeSignature = new byte[256];

        try (MockedStatic<CngKeyGuard> mockCng = Mockito.mockStatic(CngKeyGuard.class)) {
            mockCng.when(() -> CngKeyGuard.signPss(any(), any(), anyString(), anyInt()))
                   .thenReturn(fakeSignature);

            String b64 = Pkcs10Builder.generate(
                    Pointer.NULL, modulus, 65537,
                    "client-a", "tenant-b", null, null);

            byte[] der = Base64.getDecoder().decode(b64);
            String derText = new String(der, java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(derText.contains("{}"),
                    "cuId JSON must be '{}' when both vmId and vmssId are null (omitempty)");
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void generate_subjectContainsClientIdAndTenantId() throws Exception {
        BigInteger modulus = BigInteger.valueOf(2).pow(2047).add(BigInteger.ONE);
        byte[] fakeSignature = new byte[256];

        try (MockedStatic<CngKeyGuard> mockCng = Mockito.mockStatic(CngKeyGuard.class)) {
            mockCng.when(() -> CngKeyGuard.signPss(any(), any(), anyString(), anyInt()))
                   .thenReturn(fakeSignature);

            String b64 = Pkcs10Builder.generate(
                    Pointer.NULL, modulus, 65537,
                    "subject-client-id", "subject-tenant-id", null, null);

            byte[] der = Base64.getDecoder().decode(b64);
            String derText = new String(der, java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(derText.contains("subject-client-id"),
                    "CSR subject (CN) must contain the clientId");
            assertTrue(derText.contains("subject-tenant-id"),
                    "CSR subject (DC) must contain the tenantId");
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void generate_signPssCalledWithSha256AndSalt32() throws Exception {
        BigInteger modulus = BigInteger.valueOf(2).pow(2047).add(BigInteger.ONE);
        byte[] fakeSignature = new byte[256];

        try (MockedStatic<CngKeyGuard> mockCng = Mockito.mockStatic(CngKeyGuard.class)) {
            mockCng.when(() -> CngKeyGuard.signPss(any(), any(), anyString(), anyInt()))
                   .thenReturn(fakeSignature);

            Pkcs10Builder.generate(
                    Pointer.NULL, modulus, 65537,
                    "c", "t", null, null);

            // Verify the exact signature algorithm parameters (must match msal-go and MSAL.NET)
            mockCng.verify(() -> CngKeyGuard.signPss(
                    nullable(Pointer.class),
                    any(byte[].class),
                    eq("SHA256"),
                    eq(32)));
        }
    }
}
