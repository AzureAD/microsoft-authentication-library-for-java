// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Callback;
import com.sun.jna.Library;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.ptr.PointerByReference;

import java.util.Arrays;
import java.util.List;

/**
 * JNA binding for {@code AttestationClientLib.dll} — the Windows DLL shipped by Azure
 * that produces a MAA (Microsoft Azure Attestation) JWT proving a CNG KeyGuard key is
 * hardware-protected.
 *
 * <p>Function signatures (ANSI cdecl, x64 Windows) documented in MSAL.NET's
 * {@code KeyGuardMaa/AttestationInterop.cs} and also used by msal-go's
 * {@code cng_windows.go}:</p>
 * <pre>
 *   int  InitAttestationLib(AttestationLogInfo*)
 *   int  AttestKeyGuardImportKey(char* endpoint, char* authToken, char* clientPayload,
 *                                NCRYPT_KEY_HANDLE keyHandle, char** token, char* clientId)
 *   void FreeAttestationToken(char* token)
 *   void UninitAttestationLib()
 * </pre>
 *
 * <p>This interface is loaded lazily via {@link AttestationLibraryLoader} from the
 * Microsoft-signed DLL bundled in the optional extension. It is only extracted and loaded
 * when MAA attestation is requested.</p>
 */
interface AttestationLibrary extends Library {

    /**
     * No-op log callback that satisfies the DLL's requirement for a non-null LogFunc.
     *
     * <p>The DLL requires a non-null log function pointer in {@link AttestationLogInfo}.
     * Passing {@code Pointer.NULL} causes {@code InitAttestationLib} to return an error
     * (0xFFFFFFF8 = -8). Mirrors msal-go's {@code dummyLogCallback}.</p>
     *
     * <p>Signature (cdecl, x64 Windows):
     * {@code void LogFunc(void* ctx, char* tag, int lvl, char* func, int line, char* msg)}</p>
     */
    interface LogCallback extends Callback {
        void log(Pointer ctx, Pointer tag, int level, Pointer func, int line, Pointer msg);
    }

    /** Shared no-op log callback instance — kept alive to prevent GC. */
    LogCallback NOOP_LOG = (ctx, tag, level, func, line, msg) -> {};

    /**
     * Mirrors the {@code AttestationLogInfo} struct:
     * <pre>struct AttestationLogInfo { LogFunc Log; void* Ctx; }</pre>
     *
     * <p>The {@code logFunc} field MUST be a non-null function pointer — the DLL validates
     * this and returns an error if it is null. Use {@link #NOOP_LOG} for no-op logging.</p>
     */
    class AttestationLogInfo extends Structure {
        /** Function pointer for the log callback. MUST NOT be null. */
        public LogCallback logFunc;
        /** Caller context pointer, passed as first arg to logFunc. */
        public Pointer ctx;

        public AttestationLogInfo() {
            this(NOOP_LOG);
        }

        public AttestationLogInfo(LogCallback logFunc) {
            if (logFunc == null) {
                throw new NullPointerException("logFunc");
            }
            this.logFunc = logFunc;
            this.ctx = Pointer.NULL;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("logFunc", "ctx");
        }
    }

    /**
     * Initializes the attestation library.
     *
     * @param logInfo logging configuration; {@code logFunc} MUST be non-null
     * @return 0 on success, non-zero on failure
     */
    int InitAttestationLib(AttestationLogInfo logInfo);

    /**
     * Produces a MAA JWT proving the given CNG key is VBS/KeyGuard-protected.
     *
     * @param endpoint      MAA endpoint URL (ANSI string, e.g. "https://sharedcuse.cuse.attest.azure.net")
     * @param authToken     unused, pass null
     * @param clientPayload unused, pass null
     * @param keyHandle     the {@code NCRYPT_KEY_HANDLE} from NCrypt* operations
     * @param tokenOut      receives the pointer to the MAA JWT string (caller must free with FreeAttestationToken)
     * @param clientId      managed identity client ID (ANSI string)
     * @return 0 on success, non-zero on failure
     */
    int AttestKeyGuardImportKey(String endpoint, String authToken, String clientPayload,
                                Pointer keyHandle, PointerByReference tokenOut, String clientId);

    /**
     * Frees a MAA JWT string allocated by {@link #AttestKeyGuardImportKey}.
     *
     * @param token the pointer returned in {@code tokenOut} by AttestKeyGuardImportKey
     */
    void FreeAttestationToken(Pointer token);

    /** Uninitializes the attestation library. Call after all attestation operations. */
    void UninitAttestationLib();
}
