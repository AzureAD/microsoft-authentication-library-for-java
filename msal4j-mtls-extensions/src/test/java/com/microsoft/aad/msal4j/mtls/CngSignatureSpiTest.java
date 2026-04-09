// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CngSignatureSpi} — specifically the <em>delegation path</em> used
 * when a non-{@link CngRsaPrivateKey} (regular exportable RSA key) is passed to
 * {@code initSign}.
 *
 * <p>The CNG path (signing with an actual KeyGuard key) cannot be exercised in unit tests
 * without a Trusted Launch Azure VM. The delegation path, however, uses standard Java RSA
 * keys and the SunRsaSign provider, and can be fully tested without CNG hardware.</p>
 *
 * <p>Installing {@link CngProvider} at the highest priority must <em>not</em> break other
 * RSA signing operations in the same JVM — that is the contract this delegation path
 * satisfies. These tests verify that invariant.</p>
 *
 * <p>Requires Windows because loading {@link CngSignatureSpi} transitively initializes
 * {@link NCryptLibrary} ({@code ncrypt.dll}).</p>
 */
@EnabledOnOs(OS.WINDOWS)
class CngSignatureSpiTest {

    private KeyPair keyPair;

    @BeforeEach
    void generateRsaKeyPair() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        keyPair = kpg.generateKeyPair();
    }

    @AfterEach
    void removeInstalledCngProvider() {
        Security.removeProvider("CNG");
    }

    // ─── SHA256withRSA delegation ─────────────────────────────────────────────

    @Test
    void sha256WithRsa_delegation_producesVerifiableSignature() throws Exception {
        byte[] data = "hello msal-java mtls pop delegation".getBytes();

        Signature signer = Signature.getInstance("SHA256withRSA", new CngProvider());
        signer.initSign(keyPair.getPrivate()); // non-CNG key → delegation
        signer.update(data);
        byte[] sig = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(data);
        assertTrue(verifier.verify(sig),
                "SHA256withRSA signature via CNG delegation must verify with the RSA public key");
    }

    @Test
    void sha256WithRsa_delegation_tampered_fails() throws Exception {
        byte[] data    = "correct data".getBytes();
        byte[] tampered = "tampered data".getBytes();

        Signature signer = Signature.getInstance("SHA256withRSA", new CngProvider());
        signer.initSign(keyPair.getPrivate());
        signer.update(data);
        byte[] sig = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(tampered);
        assertFalse(verifier.verify(sig),
                "Signature must not verify against tampered data");
    }

    // ─── SHA1withRSA delegation ───────────────────────────────────────────────

    @Test
    void sha1WithRsa_delegation_producesVerifiableSignature() throws Exception {
        byte[] data = "hello mtls pop sha1 delegation".getBytes();

        Signature signer = Signature.getInstance("SHA1withRSA", new CngProvider());
        signer.initSign(keyPair.getPrivate());
        signer.update(data);
        byte[] sig = signer.sign();

        Signature verifier = Signature.getInstance("SHA1withRSA");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(data);
        assertTrue(verifier.verify(sig));
    }

    // ─── RSASSA-PSS delegation ─────────────────────────────────────────────────

    @Test
    void rsaSsaPss_delegation_producesVerifiableSignature() throws Exception {
        byte[] data = "hello mtls pop pss delegation".getBytes();
        PSSParameterSpec pssSpec = new PSSParameterSpec(
                "SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1);

        Signature signer = Signature.getInstance("RSASSA-PSS", new CngProvider());
        signer.initSign(keyPair.getPrivate());   // creates delegate
        signer.setParameter(pssSpec);            // forwarded to delegate
        signer.update(data);
        byte[] sig = signer.sign();

        Signature verifier = Signature.getInstance("RSASSA-PSS");
        verifier.setParameter(pssSpec);
        verifier.initVerify(keyPair.getPublic());
        verifier.update(data);
        assertTrue(verifier.verify(sig),
                "RSASSA-PSS signature via CNG delegation must verify with the RSA public key");
    }

    // ─── CngProvider installed globally does not break standard signing ────────

    @Test
    void installedCngProvider_doesNotBreakSunRsaSignSigning() throws Exception {
        CngProvider.installIfAbsent();

        byte[] data = "standard signing still works".getBytes();

        // Even with CNG at position 1 globally, explicitly requesting SunRsaSign must
        // still produce valid signatures. This verifies that installIfAbsent() is
        // non-destructive to the global Security provider list.
        Signature signer = Signature.getInstance("SHA256withRSA", "SunRsaSign");
        signer.initSign(keyPair.getPrivate());
        signer.update(data);
        byte[] sig = signer.sign();

        Signature verifier = Signature.getInstance("SHA256withRSA", "SunRsaSign");
        verifier.initVerify(keyPair.getPublic());
        verifier.update(data);
        assertTrue(verifier.verify(sig),
                "SunRsaSign signing must work correctly even when CNG is installed globally at position 1");
    }
}
