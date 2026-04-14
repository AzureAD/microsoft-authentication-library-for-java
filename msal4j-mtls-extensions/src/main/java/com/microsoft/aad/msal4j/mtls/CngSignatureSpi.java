// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Security;
import java.security.SignatureException;
import java.security.SignatureSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;

/**
 * {@link SignatureSpi} implementations that delegate signing to Windows CNG via JNA.
 *
 * <p>Two families are provided:</p>
 * <ul>
 *   <li>{@link Sha256WithRsa} / {@link Sha1WithRsa} — PKCS#1 v1.5 padding
 *       (TLS 1.2 client cert verify, and CSR signing fallback)</li>
 *   <li>{@link RsaSsaPss} — RSASSA-PSS with configurable parameters
 *       (TLS 1.3 client cert verify)</li>
 * </ul>
 *
 * <p>For non-{@link CngRsaPrivateKey} keys, each SPI delegates to the next available
 * provider so that installing {@link CngProvider} at high priority does not break other
 * code in the same JVM that signs with regular (exportable) RSA keys.</p>
 */
abstract class CngSignatureSpi extends SignatureSpi {

    // ─── Concrete algorithms ───────────────────────────────────────────────────

    /** SHA-256 with RSA PKCS#1 v1.5 */
    public static class Sha256WithRsa extends CngSignatureSpi {
        public Sha256WithRsa() { super("SHA-256", "SHA256", false, 32); }
    }

    /** SHA-1 with RSA PKCS#1 v1.5 */
    public static class Sha1WithRsa extends CngSignatureSpi {
        public Sha1WithRsa() { super("SHA-1", "SHA1", false, 20); }
    }

    /** RSASSA-PSS — algorithm parameters set via {@link #engineSetParameter(AlgorithmParameterSpec)} */
    public static class RsaSsaPss extends CngSignatureSpi {
        public RsaSsaPss() { super("SHA-256", "SHA256", true, 32); }
    }

    // ─── State ────────────────────────────────────────────────────────────────

    private final boolean pss;

    // CNG mode
    private Pointer cngHandle;
    private MessageDigest digest;
    private String hashJce;   // Java algorithm name (e.g. "SHA-256")
    private String hashCng;   // CNG algorithm name (e.g. "SHA256")
    private int    saltLen;

    // Delegation mode (non-CNG keys)
    private java.security.Signature delegate;

    CngSignatureSpi(String hashJce, String hashCng, boolean pss, int saltLen) {
        this.hashJce  = hashJce;
        this.hashCng  = hashCng;
        this.pss      = pss;
        this.saltLen  = saltLen;
    }

    // ─── SignatureSpi ─────────────────────────────────────────────────────────

    @Override
    protected void engineInitVerify(java.security.PublicKey publicKey)
            throws InvalidKeyException {
        // CNG only handles signing (NCryptSignHash). For verification (server cert
        // validation, etc.) we deliberately throw InvalidKeyException so that
        // Signature.Delegate.chooseProvider() skips this SPI and falls through to
        // SunRsaSign or another standard provider that handles RSA/ECDSA verification.
        throw new InvalidKeyException(
                "CngSignatureSpi does not support verification; use SunRsaSign");
    }

    @Override
    protected void engineInitSign(PrivateKey key) throws InvalidKeyException {
        if (key instanceof CngRsaPrivateKey) {
            Pointer h;
            try {
                h = ((CngRsaPrivateKey) key).getHandle();
            } catch (IllegalStateException e) {
                throw new InvalidKeyException("CNG key is closed: " + e.getMessage(), e);
            }
            if (h == null) {
                throw new InvalidKeyException("CNG key handle is null (key may be closed or invalid)");
            }
            cngHandle = h;
            delegate  = null;
            try {
                digest = MessageDigest.getInstance(hashJce);
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidKeyException("MessageDigest " + hashJce + " not available", e);
            }
        } else {
            // Delegate to the next provider that handles this algorithm.
            cngHandle = null;
            delegate   = null;
            try {
                delegate = getNextProviderSignature();
                delegate.initSign(key);
            } catch (NoSuchAlgorithmException e) {
                throw new InvalidKeyException("No fallback provider: " + e.getMessage(), e);
            }
        }
    }

    @Override
    protected void engineUpdate(byte b) throws SignatureException {
        if (cngHandle != null) {
            digest.update(b);
        } else if (delegate != null) {
            delegate.update(b);
        } else {
            throw new SignatureException(
                    "CngSignatureSpi.engineUpdate called before engineInitSign — " +
                    "Signature object was not properly initialized");
        }
    }

    @Override
    protected void engineUpdate(byte[] b, int off, int len) throws SignatureException {
        if (cngHandle != null) {
            digest.update(b, off, len);
        } else if (delegate != null) {
            delegate.update(b, off, len);
        } else {
            throw new SignatureException(
                    "CngSignatureSpi.engineUpdate called before engineInitSign — " +
                    "Signature object was not properly initialized");
        }
    }

    @Override
    protected byte[] engineSign() throws SignatureException {
        if (cngHandle != null) {
            byte[] hash = digest.digest();
            try {
                if (pss) {
                    return CngKeyGuard.signPss(cngHandle, hash, hashCng, saltLen);
                } else {
                    return CngKeyGuard.signPkcs1(cngHandle, hash, hashCng);
                }
            } catch (MtlsMsiException e) {
                throw new SignatureException("CNG signing failed: " + e.getMessage(), e);
            }
        } else {
            return delegate.sign();
        }
    }

    @Override
    protected boolean engineVerify(byte[] sigBytes) throws SignatureException {
        if (delegate != null) {
            return delegate.verify(sigBytes);
        }
        // Verification is not needed for client-auth TLS or CSR generation.
        throw new SignatureException("CngSignatureSpi does not support verify (CNG-backed keys)");
    }

    @Override
    protected void engineSetParameter(AlgorithmParameterSpec params)
            throws InvalidAlgorithmParameterException {
        if (params instanceof PSSParameterSpec) {
            PSSParameterSpec pssSpec = (PSSParameterSpec) params;
            hashJce  = pssSpec.getDigestAlgorithm();
            hashCng  = toCngHashName(pssSpec.getDigestAlgorithm());
            saltLen  = pssSpec.getSaltLength();
            if (cngHandle != null) {
                // Re-initialize the digest with the new hash algorithm.
                try {
                    digest = MessageDigest.getInstance(hashJce);
                } catch (NoSuchAlgorithmException e) {
                    throw new InvalidAlgorithmParameterException(
                            "MessageDigest " + hashJce + " not available", e);
                }
            } else if (delegate != null) {
                // Forward PSS params to the delegating provider's Signature instance.
                try {
                    delegate.setParameter(params);
                } catch (Exception e) {
                    throw new InvalidAlgorithmParameterException(e.getMessage(), e);
                }
            }
        } else if (delegate != null && params != null) {
            try {
                delegate.setParameter(params);
            } catch (Exception e) {
                throw new InvalidAlgorithmParameterException(e.getMessage(), e);
            }
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void engineSetParameter(String param, Object value) {
        // Legacy method — no-op, required by abstract superclass.
    }

    @Override
    @SuppressWarnings("deprecation")
    protected Object engineGetParameter(String param) {
        return null;
    }

    @Override
    protected AlgorithmParameters engineGetParameters() {
        if (pss && delegate == null) {
            try {
                AlgorithmParameters ap = AlgorithmParameters.getInstance("RSASSA-PSS");
                ap.init(new PSSParameterSpec(hashJce, "MGF1",
                        new MGF1ParameterSpec(hashJce), saltLen, 1));
                return ap;
            } catch (Exception e) {
                return null;
            }
        }
        if (delegate != null) {
            return delegate.getParameters();
        }
        return null;
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private java.security.Signature getNextProviderSignature() throws NoSuchAlgorithmException {
        String algName = pss ? "RSASSA-PSS" : (hashCng.equals("SHA256") ? "SHA256withRSA" : "SHA1withRSA");
        for (Provider p : Security.getProviders()) {
            if (p instanceof CngProvider) continue;
            if (p.getService("Signature", algName) != null) {
                return java.security.Signature.getInstance(algName, p);
            }
        }
        throw new NoSuchAlgorithmException(
                "No provider for " + algName + " besides CngProvider");
    }

    private static String toCngHashName(String jceHashName) {
        if (jceHashName == null) return "SHA256";
        switch (jceHashName.toUpperCase().replace("-", "")) {
            case "SHA1":   return "SHA1";
            case "SHA256": return "SHA256";
            case "SHA384": return "SHA384";
            case "SHA512": return "SHA512";
            default:       return "SHA256";
        }
    }
}
