// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

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
 * <p>This interface is loaded lazily via {@link CngKeyGuard} — it is only required when
 * MAA attestation is requested and the DLL is present on the system. If the DLL is absent
 * and attestation is not requested, it is never loaded.</p>
 */
interface AttestationLibrary extends Library {

    /**
     * Mirrors the {@code AttestationLogInfo} struct:
     * <pre>struct AttestationLogInfo { void* LogFunc; void* Ctx; }</pre>
     * Pass zero values to disable logging.
     */
    class AttestationLogInfo extends Structure {
        /** Function pointer for the log callback. Use zero/null for no-op. */
        public Pointer logFunc;
        /** Caller context pointer, passed as first arg to logFunc. */
        public Pointer ctx;

        public AttestationLogInfo() {
            logFunc = Pointer.NULL;
            ctx     = Pointer.NULL;
        }

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList("logFunc", "ctx");
        }
    }

    /**
     * Initializes the attestation library.
     *
     * @param logInfo logging configuration; pass a zeroed struct to disable
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
