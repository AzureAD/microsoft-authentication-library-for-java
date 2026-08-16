// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.security.Provider;
import java.security.Security;
import java.security.Signature;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CngProvider} — provider registration and service declarations.
 *
 * <p>Requires Windows because loading {@link CngProvider} transitively loads
 * {@link CngSignatureSpi} → {@link CngRsaPrivateKey} → {@link NCryptLibrary}
 * ({@code ncrypt.dll}).</p>
 */
@EnabledOnOs(OS.WINDOWS)
class CngProviderTest {

    @AfterEach
    void removeCngProvider() {
        // Remove after each test to prevent state from bleeding between tests
        Security.removeProvider("CNG");
    }

    // ─── installIfAbsent ─────────────────────────────────────────────────────

    @Test
    void installIfAbsent_registersProviderByName() {
        CngProvider.installIfAbsent();
        assertNotNull(Security.getProvider("CNG"),
                "CNG provider must be registered in the JVM Security list after installIfAbsent()");
    }

    @Test
    void installIfAbsent_isIdempotent() {
        CngProvider.installIfAbsent();
        CngProvider.installIfAbsent(); // second call must be a no-op

        long cngCount = Arrays.stream(Security.getProviders())
                .filter(p -> "CNG".equals(p.getName()))
                .count();
        assertEquals(1, cngCount,
                "CNG provider must appear exactly once even after multiple installIfAbsent() calls");
    }

    @Test
    void installIfAbsent_insertsAtHighestPriority() {
        CngProvider.installIfAbsent();
        Provider[] providers = Security.getProviders();
        // Security position 1 = index 0 in the array
        assertEquals("CNG", providers[0].getName(),
                "CNG must be at Security position 1 (highest priority) so JSSE uses it first");
    }

    // ─── Service registrations ────────────────────────────────────────────────

    @Test
    void provider_registersSha256WithRsa() {
        Provider p = new CngProvider();
        assertNotNull(p.getService("Signature", "SHA256withRSA"),
                "CNG provider must advertise SHA256withRSA (used by TLS 1.2 client cert verify)");
    }

    @Test
    void provider_registersSha384WithRsa() {
        Provider p = new CngProvider();
        assertNotNull(p.getService("Signature", "SHA384withRSA"));
    }

    @Test
    void provider_registersSha512WithRsa() {
        Provider p = new CngProvider();
        assertNotNull(p.getService("Signature", "SHA512withRSA"));
    }

    @Test
    void provider_registersRsaSsaPss() {
        Provider p = new CngProvider();
        assertNotNull(p.getService("Signature", "RSASSA-PSS"),
                "CNG provider must advertise RSASSA-PSS");
    }

    @Test
    void provider_name_isCng() {
        assertEquals("CNG", new CngProvider().getName());
    }

    @Test
    void provider_sha256Alias_resolves() {
        Provider p = new CngProvider();
        // Alias "SHA-256withRSA" must resolve to "SHA256withRSA"
        assertNotNull(p.getService("Signature", "SHA-256withRSA"),
                "Alias SHA-256withRSA must resolve via the CNG provider");
    }

    @Test
    void installedProvider_bypassesOrdinaryRsaKeys() throws Exception {
        CngProvider.installIfAbsent();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(keyPair.getPrivate());

        assertNotEquals("CNG", signature.getProvider().getName());
    }
}
