// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;

import java.math.BigInteger;
import java.security.interfaces.RSAPrivateKey;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A non-exportable RSA private key backed by a Windows CNG {@code NCRYPT_KEY_HANDLE}.
 *
 * <p>This key implements {@link RSAPrivateKey} so that JSSE recognizes it as an RSA key
 * and selects RSA cipher suites. The private exponent is {@code null} and
 * {@link #getEncoded()} returns {@code null} because the key material never leaves the
 * CNG key storage provider (KeyGuard VBS isolation).</p>
 *
 * <p>Signing is performed by {@link CngKeyGuard#signPkcs1} / {@link CngKeyGuard#signPss},
 * dispatched from {@link CngSignatureSpi}.</p>
 *
 * <p>Callers must call {@link #close()} when done to free the CNG handle.</p>
 */
public final class CngRsaPrivateKey implements RSAPrivateKey, AutoCloseable {

    private static final long serialVersionUID = 1L;

    private final Pointer handle;
    private final BigInteger modulus;
    private final int publicExponent;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final Runnable releaser;

    CngRsaPrivateKey(Pointer handle, BigInteger modulus, int publicExponent) {
        this(handle, modulus, publicExponent,
                () -> NCryptLibrary.INSTANCE.NCryptFreeObject(handle));
    }

    CngRsaPrivateKey(
            Pointer handle,
            BigInteger modulus,
            int publicExponent,
            Runnable releaser) {
        this.handle        = handle;
        this.modulus       = modulus;
        this.publicExponent = publicExponent;
        this.releaser = releaser;
    }

    Pointer nativeHandle() {
        if (closed.get()) throw new IllegalStateException("CNG key handle has been closed");
        return handle;
    }

    // ─── RSAKey ───────────────────────────────────────────────────────────────

    /** Returns the RSA modulus (from the exported RSAPUBLICBLOB — public information). */
    @Override
    public BigInteger getModulus() {
        return modulus;
    }

    /**
     * Always returns {@code null}. The private exponent is non-exportable from the
     * KeyGuard-protected CNG key; signing is delegated to {@code NCryptSignHash}.
     */
    @Override
    public BigInteger getPrivateExponent() {
        return null;
    }

    // ─── Key ──────────────────────────────────────────────────────────────────

    @Override
    public String getAlgorithm() { return "RSA"; }

    /** Returns {@code null} — non-exportable key has no serializable encoding. */
    @Override
    public String getFormat() { return null; }

    /** Returns {@code null} — non-exportable key has no serializable encoding. */
    @Override
    public byte[] getEncoded() { return null; }

    // ─── AutoCloseable ────────────────────────────────────────────────────────

    /**
     * Frees the underlying CNG key handle via {@code NCryptFreeObject}.
     * The key remains persisted in the KSP; only the in-process handle is released.
     */
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            releaser.run();
        }
    }

    boolean isClosed() {
        return closed.get();
    }

    /** The public exponent (e.g. 65537 = 0x10001). */
    public int getPublicExponent() {
        return publicExponent;
    }
}
