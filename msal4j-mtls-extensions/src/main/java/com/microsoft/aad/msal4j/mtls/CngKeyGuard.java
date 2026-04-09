// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Windows CNG key operations for mTLS PoP Managed Identity.
 *
 * <p>Mirrors msal-go's {@code cng_windows.go} and MSAL.NET's
 * {@code WindowsManagedIdentityKeyProvider}: creates or opens a persisted RSA key in
 * the Microsoft Software Key Storage Provider, using the same 3-level priority:</p>
 * <ol>
 *   <li><strong>KeyGuard</strong> — Software KSP + USER scope + VBS Virtual Isolation flags.
 *       Requires Credential Guard / Core Isolation on the VM.</li>
 *   <li><strong>Hardware</strong> — Software KSP + USER scope, no VBS flags.</li>
 *   <li>mTLS PoP requires KeyGuard and throws {@link MtlsMsiException} for Hardware keys.</li>
 * </ol>
 */
public final class CngKeyGuard {

    private static final String MS_SOFTWARE_KSP = "Microsoft Software Key Storage Provider";
    private static final String RSA_ALG          = "RSA";
    private static final String EXPORT_POLICY    = "Export Policy";
    private static final String KEY_LENGTH       = "Length";
    private static final String VIRTUAL_ISO      = "Virtual Iso";
    private static final String RSAPUBLICBLOB    = "RSAPUBLICBLOB";

    private CngKeyGuard() {}

    /**
     * Gets or creates the mTLS PoP binding key, attempting KeyGuard first.
     *
     * @param keyName persisted key name in the KSP (e.g. {@code "MSALMtlsKey_<vmId>"})
     * @return a {@link CngRsaPrivateKey} backed by the CNG handle
     * @throws MtlsMsiException if the system is not Windows, the key cannot be created,
     *                          or KeyGuard protection is unavailable
     */
    public static CngRsaPrivateKey getOrCreateKey(String keyName) throws MtlsMsiException {
        if (!isWindows()) {
            throw new MtlsMsiException("mTLS PoP Managed Identity is only supported on Windows Azure VMs.");
        }

        // 1. Try KeyGuard (USER scope + VBS Virtual Isolation flags).
        int kgCreateFlags = NCryptLibrary.NCRYPT_OVERWRITE_KEY_FLAG
                          | NCryptLibrary.NCRYPT_USE_VIRTUAL_ISOLATION_FLAG
                          | NCryptLibrary.NCRYPT_USE_PER_BOOT_KEY_FLAG;
        try {
            CngRsaPrivateKey key = tryGetOrCreateKey(keyName, NCryptLibrary.NCRYPT_SILENT_FLAG, kgCreateFlags, NCryptLibrary.NCRYPT_SILENT_FLAG);
            if (isKeyGuardProtected(key.getHandle())) {
                return key;
            }
            // Created but VBS protection not active — delete and retry once (mirrors MSAL.NET).
            NCryptLibrary.INSTANCE.NCryptDeleteKey(key.getHandle(), 0);
            key = tryGetOrCreateKey(keyName, NCryptLibrary.NCRYPT_SILENT_FLAG, kgCreateFlags, NCryptLibrary.NCRYPT_SILENT_FLAG);
            if (isKeyGuardProtected(key.getHandle())) {
                return key;
            }
            NCryptLibrary.INSTANCE.NCryptFreeObject(key.getHandle());
        } catch (MtlsMsiException ignored) {
            // KeyGuard not available on this VM; fall through to error below.
        }

        throw new MtlsMsiException(
                "mTLS PoP requires a VBS KeyGuard-protected RSA key, but KeyGuard is not available " +
                "on this VM. Ensure Credential Guard / Core Isolation is enabled: the VM must be " +
                "Trusted Launch (Secure Boot + vTPM) with VBS active " +
                "(check msinfo32.exe: 'Virtualization-based security' = Running).");
    }

    /**
     * Produces a MAA JWT by calling {@code AttestationClientLib.dll}.
     *
     * @param keyHandle       CNG key handle from {@link CngRsaPrivateKey#getHandle()}
     * @param endpoint        MAA attestation endpoint URL (from IMDS platform metadata)
     * @param clientId        managed identity client ID (from IMDS platform metadata)
     * @return the MAA JWT string
     * @throws MtlsMsiException if the DLL is not present, or attestation fails
     */
    public static String getAttestationToken(Pointer keyHandle, String endpoint, String clientId)
            throws MtlsMsiException {

        AttestationLibrary attestLib;
        try {
            attestLib = Native.load("AttestationClientLib", AttestationLibrary.class);
        } catch (UnsatisfiedLinkError e) {
            throw new MtlsMsiException(
                    "AttestationClientLib.dll not found. Place the DLL in a directory on the system PATH " +
                    "or in the same directory as the JVM. " +
                    "Obtain it from the Microsoft.Azure.Security.KeyGuardAttestation NuGet package " +
                    "(runtimes/win-x64/native/AttestationClientLib.dll). Error: " + e.getMessage(), e);
        }

        AttestationLibrary.AttestationLogInfo logInfo = new AttestationLibrary.AttestationLogInfo();
        int ret = attestLib.InitAttestationLib(logInfo);
        if (ret != 0) {
            throw new MtlsMsiException(
                    String.format("InitAttestationLib failed: 0x%x", ret));
        }

        try {
            PointerByReference tokenRef = new PointerByReference();
            ret = attestLib.AttestKeyGuardImportKey(endpoint, null, null, keyHandle, tokenRef, clientId);
            if (ret != 0) {
                throw new MtlsMsiException(String.format(
                        "AttestKeyGuardImportKey failed (rc=0x%x). This usually means the VM's vTPM " +
                        "is not provisioned for attestation. mTLS PoP requires a Trusted Launch Azure VM " +
                        "(Secure Boot + vTPM) with an EK certificate. " +
                        "Check 'tpmtool.exe getdeviceinformation': 'Is Capable For Attestation' must be true.", ret));
            }

            Pointer tokenPtr = tokenRef.getValue();
            if (tokenPtr == null || tokenPtr == Pointer.NULL) {
                throw new MtlsMsiException("AttestKeyGuardImportKey returned null token");
            }

            try {
                String jwt = tokenPtr.getString(0); // ANSI (null-terminated)
                if (jwt == null || jwt.isEmpty()) {
                    throw new MtlsMsiException("AttestKeyGuardImportKey returned empty token");
                }
                return jwt;
            } finally {
                attestLib.FreeAttestationToken(tokenPtr);
            }
        } finally {
            attestLib.UninitAttestationLib();
        }
    }

    /**
     * Signs a digest using {@code NCryptSignHash} with PKCS#1 v1.5 padding.
     *
     * @param keyHandle  CNG key handle
     * @param digest     the hash bytes to sign
     * @param hashAlgCng CNG hash algorithm name (e.g. {@code "SHA256"})
     * @return DER-encoded signature bytes
     * @throws MtlsMsiException if signing fails
     */
    public static byte[] signPkcs1(Pointer keyHandle, byte[] digest, String hashAlgCng)
            throws MtlsMsiException {
        NCryptLibrary.BcryptPkcs1PaddingInfo padding =
                new NCryptLibrary.BcryptPkcs1PaddingInfo(hashAlgCng);
        return ncryptSign(keyHandle, padding.getPointer(), NCryptLibrary.NCRYPT_PAD_PKCS1_FLAG, digest, "PKCS1v15");
    }

    /**
     * Signs a digest using {@code NCryptSignHash} with RSASSA-PSS padding.
     *
     * @param keyHandle  CNG key handle
     * @param digest     the hash bytes to sign
     * @param hashAlgCng CNG hash algorithm name (e.g. {@code "SHA256"})
     * @param saltLen    PSS salt length in bytes
     * @return DER-encoded signature bytes
     * @throws MtlsMsiException if signing fails
     */
    public static byte[] signPss(Pointer keyHandle, byte[] digest, String hashAlgCng, int saltLen)
            throws MtlsMsiException {
        NCryptLibrary.BcryptPssPaddingInfo padding =
                new NCryptLibrary.BcryptPssPaddingInfo(hashAlgCng, saltLen);
        return ncryptSign(keyHandle, padding.getPointer(), NCryptLibrary.NCRYPT_PAD_PSS_FLAG, digest, "PSS");
    }

    private static byte[] ncryptSign(Pointer hKey, Pointer paddingPtr, int paddingFlag,
                                      byte[] digest, String label) throws MtlsMsiException {
        IntByReference sigLen = new IntByReference(0);
        // First call: query the signature buffer size.
        int ret = NCryptLibrary.INSTANCE.NCryptSignHash(
                hKey, paddingPtr, digest, digest.length,
                null, 0, sigLen, paddingFlag);
        if (ret != NCryptLibrary.ERROR_SUCCESS) {
            throw new MtlsMsiException(
                    String.format("NCryptSignHash %s (size query) failed: 0x%x", label, ret));
        }

        Memory sigBuf = new Memory(sigLen.getValue());
        ret = NCryptLibrary.INSTANCE.NCryptSignHash(
                hKey, paddingPtr, digest, digest.length,
                sigBuf, sigLen.getValue(), sigLen, paddingFlag);
        if (ret != NCryptLibrary.ERROR_SUCCESS) {
            throw new MtlsMsiException(
                    String.format("NCryptSignHash %s failed: 0x%x", label, ret));
        }

        return sigBuf.getByteArray(0, sigLen.getValue());
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private static CngRsaPrivateKey tryGetOrCreateKey(String keyName,
                                                       int openFlags,
                                                       int createFlags,
                                                       int finalizeFlags) throws MtlsMsiException {
        Pointer hProvider = openProvider();
        try {
            WString keyNameW = new WString(keyName);

            // 1. Try to open an existing key.
            PointerByReference phKey = new PointerByReference();
            int ret = NCryptLibrary.INSTANCE.NCryptOpenKey(
                    hProvider, phKey, keyNameW, 0, openFlags);

            Pointer hKey;
            if (ret == NCryptLibrary.ERROR_SUCCESS) {
                hKey = phKey.getValue();
                // Verify the key is usable by exporting the public blob.
                try {
                    exportPublicKeyBytes(hKey);
                } catch (MtlsMsiException e) {
                    NCryptLibrary.INSTANCE.NCryptDeleteKey(hKey, 0);
                    hKey = null;
                    ret = -1;
                }
            } else {
                hKey = null;
            }

            // 2. Create a new key if open failed.
            if (hKey == null) {
                ret = NCryptLibrary.INSTANCE.NCryptCreatePersistedKey(
                        hProvider, phKey,
                        new WString(RSA_ALG),
                        keyNameW,
                        0, createFlags);
                if (ret != NCryptLibrary.ERROR_SUCCESS) {
                    throw new MtlsMsiException(
                            String.format("NCryptCreatePersistedKey failed: 0x%x", ret));
                }
                hKey = phKey.getValue();

                // Set key length to 2048.
                setDwordProperty(hKey, KEY_LENGTH, 2048);
                // Set non-exportable.
                setDwordProperty(hKey, EXPORT_POLICY, NCryptLibrary.NCRYPT_ALLOW_EXPORT_NONE);

                ret = NCryptLibrary.INSTANCE.NCryptFinalizeKey(hKey, finalizeFlags);
                if (ret != NCryptLibrary.ERROR_SUCCESS) {
                    NCryptLibrary.INSTANCE.NCryptDeleteKey(hKey, 0);
                    throw new MtlsMsiException(
                            String.format("NCryptFinalizeKey failed: 0x%x. " +
                                    "VBS isolation flags are not supported on this machine " +
                                    "(Credential Guard / Core Isolation not active).", ret));
                }
            }

            BigInteger[] pubKey = exportPublicKey(hKey);
            return new CngRsaPrivateKey(hKey, pubKey[0], pubKey[1].intValue());

        } finally {
            NCryptLibrary.INSTANCE.NCryptFreeObject(hProvider);
        }
    }

    static boolean isKeyGuardProtected(Pointer hKey) {
        WString propW = new WString(VIRTUAL_ISO);
        byte[] buf = new byte[4];
        IntByReference pcbResult = new IntByReference(0);
        int ret = NCryptLibrary.INSTANCE.NCryptGetProperty(
                hKey, propW, buf, buf.length, pcbResult, 0);
        if (ret != NCryptLibrary.ERROR_SUCCESS || pcbResult.getValue() < 4) {
            return false;
        }
        int val = ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).getInt();
        return val != 0;
    }

    /** Returns byte[] of the RSAPUBLICBLOB for use in CSR SubjectPublicKeyInfo. */
    static byte[] exportPublicKeyBytes(Pointer hKey) throws MtlsMsiException {
        WString blobType = new WString(RSAPUBLICBLOB);
        IntByReference pcbResult = new IntByReference(0);

        // Query size.
        int ret = NCryptLibrary.INSTANCE.NCryptExportKey(
                hKey, null, blobType, null, null, 0, pcbResult, 0);
        if (ret != NCryptLibrary.ERROR_SUCCESS) {
            throw new MtlsMsiException(
                    String.format("NCryptExportKey (size query) failed: 0x%x", ret));
        }

        Memory blob = new Memory(pcbResult.getValue());
        ret = NCryptLibrary.INSTANCE.NCryptExportKey(
                hKey, null, blobType, null, blob, pcbResult.getValue(), pcbResult, 0);
        if (ret != NCryptLibrary.ERROR_SUCCESS) {
            throw new MtlsMsiException(
                    String.format("NCryptExportKey failed: 0x%x", ret));
        }

        return blob.getByteArray(0, pcbResult.getValue());
    }

    /**
     * Returns [modulus, publicExponent] parsed from the RSAPUBLICBLOB.
     * BCRYPT_RSAKEY_BLOB format (24-byte header):
     * <pre>Magic(4) BitLength(4) cbPublicExp(4) cbModulus(4) cbPrime1(4) cbPrime2(4)</pre>
     * followed by PublicExponent bytes then Modulus bytes.
     */
    static BigInteger[] exportPublicKey(Pointer hKey) throws MtlsMsiException {
        byte[] blob = exportPublicKeyBytes(hKey);
        if (blob.length < 24) {
            throw new MtlsMsiException("RSAPUBLICBLOB too short: " + blob.length);
        }

        ByteBuffer bb = ByteBuffer.wrap(blob).order(ByteOrder.LITTLE_ENDIAN);
        bb.getInt(); // magic
        bb.getInt(); // bitLength
        int cbPublicExp = bb.getInt();
        int cbModulus   = bb.getInt();
        // skip cbPrime1, cbPrime2
        bb.position(24);

        byte[] expBytes = new byte[cbPublicExp];
        bb.get(expBytes);
        byte[] modBytes = new byte[cbModulus];
        bb.get(modBytes);

        return new BigInteger[] {
            new BigInteger(1, modBytes),   // [0] = modulus
            new BigInteger(1, expBytes)    // [1] = publicExponent
        };
    }

    private static Pointer openProvider() throws MtlsMsiException {
        PointerByReference phProvider = new PointerByReference();
        int ret = NCryptLibrary.INSTANCE.NCryptOpenStorageProvider(
                phProvider, new WString(MS_SOFTWARE_KSP), 0);
        if (ret != NCryptLibrary.ERROR_SUCCESS) {
            throw new MtlsMsiException(
                    String.format("NCryptOpenStorageProvider failed: 0x%x", ret));
        }
        return phProvider.getValue();
    }

    private static void setDwordProperty(Pointer hKey, String propName, int value)
            throws MtlsMsiException {
        byte[] buf = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array();
        int ret = NCryptLibrary.INSTANCE.NCryptSetProperty(
                hKey, new WString(propName), buf, buf.length, NCryptLibrary.NCRYPT_SILENT_FLAG);
        if (ret != NCryptLibrary.ERROR_SUCCESS) {
            throw new MtlsMsiException(
                    String.format("NCryptSetProperty(%s) failed: 0x%x", propName, ret));
        }
    }

    private static boolean isWindows() {
        String os = System.getProperty("os.name", "").toLowerCase();
        return os.contains("windows");
    }
}
