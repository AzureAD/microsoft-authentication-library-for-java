// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

/**
 * JNA binding for {@code ncrypt.dll} — Windows CNG (Cryptography Next Generation) key
 * storage and signing operations.
 *
 * <p>Function signatures mirror MSAL.NET's {@code WindowsCngKeyOperations} and msal-go's
 * {@code cng_windows.go}. All NCrypt functions follow the Windows x64 calling convention
 * (which equals cdecl on x64).</p>
 */
interface NCryptLibrary extends Library {

    NCryptLibrary INSTANCE = Native.load("ncrypt", NCryptLibrary.class);

    // ─── NCrypt constants ──────────────────────────────────────────────────────

    int ERROR_SUCCESS                    = 0;

    int NCRYPT_SILENT_FLAG               = 0x00000040;
    int NCRYPT_OVERWRITE_KEY_FLAG        = 0x00000080;
    int NCRYPT_MACHINE_KEY_FLAG          = 0x00000020;  // not used (USER scope only)
    int NCRYPT_USE_VIRTUAL_ISOLATION_FLAG = 0x00020000; // VBS KeyGuard
    int NCRYPT_USE_PER_BOOT_KEY_FLAG     = 0x00040000;  // ephemeral per boot
    int NCRYPT_ALLOW_EXPORT_NONE         = 0;            // non-exportable

    int NCRYPT_PAD_PKCS1_FLAG            = 0x00000002;
    int NCRYPT_PAD_PSS_FLAG              = 0x00000008;

    // ─── Padding info structures ───────────────────────────────────────────────

    /** Maps to {@code BCRYPT_PKCS1_PADDING_INFO} — used with NCRYPT_PAD_PKCS1_FLAG. */
    class BcryptPkcs1PaddingInfo extends Structure {
        /** Algorithm name for the hash (e.g. L"SHA256"). LPCWSTR in C. */
        public WString pszAlgId;

        public BcryptPkcs1PaddingInfo(String algName) {
            pszAlgId = new WString(algName);
            write();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("pszAlgId");
        }
    }

    /** Maps to {@code BCRYPT_PSS_PADDING_INFO} — used with NCRYPT_PAD_PSS_FLAG. */
    class BcryptPssPaddingInfo extends Structure {
        /** Algorithm name for the hash (e.g. L"SHA256"). LPCWSTR in C. */
        public WString pszAlgId;
        /** Salt length in bytes. Typically equals hash output length for RSASSA-PSS. */
        public int cbSalt;

        public BcryptPssPaddingInfo(String algName, int saltLen) {
            pszAlgId = new WString(algName);
            cbSalt   = saltLen;
            write();
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("pszAlgId", "cbSalt");
        }
    }

    // ─── NCrypt API ────────────────────────────────────────────────────────────

    int NCryptOpenStorageProvider(PointerByReference phProvider, WString pszProviderName, int dwFlags);

    int NCryptOpenKey(Pointer hProvider, PointerByReference phKey, WString pszKeyName,
                      int dwLegacyKeySpec, int dwFlags);

    int NCryptCreatePersistedKey(Pointer hProvider, PointerByReference phKey,
                                  WString pszAlgId, WString pszKeyName,
                                  int dwLegacyKeySpec, int dwFlags);

    int NCryptSetProperty(Pointer hObject, WString pszProperty,
                          byte[] pbInput, int cbInput, int dwFlags);

    int NCryptGetProperty(Pointer hObject, WString pszProperty,
                          byte[] pbOutput, int cbOutput,
                          IntByReference pcbResult, int dwFlags);

    int NCryptFinalizeKey(Pointer hKey, int dwFlags);

    /** First call: pass {@code pbOutput=null, cbOutput=0} to query required buffer size. */
    int NCryptExportKey(Pointer hKey, Pointer hExportKey, WString pszBlobType,
                        Pointer pParameterList, Pointer pbOutput, int cbOutput,
                        IntByReference pcbResult, int dwFlags);

    /**
     * First call: pass {@code pbSignature=null, cbSignature=0} to get required buffer size
     * (returned in {@code pcbResult}).
     * Second call: pass a {@code Memory} buffer of that size.
     */
    int NCryptSignHash(Pointer hKey, Pointer pPaddingInfo,
                       byte[] pbHashValue, int cbHashValue,
                       Pointer pbSignature, int cbSignature,
                       IntByReference pcbResult, int dwFlags);

    int NCryptFreeObject(Pointer hObject);

    int NCryptDeleteKey(Pointer hKey, int dwFlags);
}
