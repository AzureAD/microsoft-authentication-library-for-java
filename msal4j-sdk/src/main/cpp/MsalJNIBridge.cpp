// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

/**
 * MsalJNIBridge.cpp - Native implementation for MSAL Java MSI v2 mTLS PoP support.
 *
 * This file implements the JNI bridge between MSAL Java and the Windows platform
 * for MSI v2 (mTLS Proof-of-Possession with KeyGuard attestation).
 *
 * Implementation overview:
 *  - createKeyGuardRsaKeyNative: Creates a VBS-isolated per-boot RSA key via NCrypt.
 *    The key is non-exportable and protected by Virtualization Based Security (VBS).
 *  - getPublicKeyNative: Exports the public key in DER/SubjectPublicKeyInfo format.
 *  - signWithKeyGuardNative: Signs data using the hardware-protected key (RSA-PSS/SHA-256).
 *  - getAttestationTokenNative: Calls AttestationClientLib.dll to obtain an attestation JWT.
 *  - acquireMtlsTokenNative: Performs an mTLS HTTPS POST using the hardware key as client cert.
 *  - freeKeyHandleNative: Frees the native NCrypt key handle.
 *
 * Build Requirements:
 *  - Visual Studio 2019+ or MSVC toolchain
 *  - Windows SDK 10.0.19041.0+
 *  - Links: ncrypt.lib, bcrypt.lib, winhttp.lib, crypt32.lib
 *  - JNI headers from the JDK include directory
 *  - AttestationClientLib.lib (for attestation support)
 *
 * NOTE: This file is a stub implementation. Full implementation requires
 *       the AttestationClientLib.dll and Windows VBS-enabled environment.
 */

#include "MsalJNIBridge.h"
#include <windows.h>
#include <ncrypt.h>
#include <bcrypt.h>
#include <wincrypt.h>
#include <jni.h>
#include <vector>
#include <string>
#include <stdexcept>

// ============================================================================
// Helper utilities
// ============================================================================

/**
 * Deserializes the opaque key handle byte array back to NCRYPT_KEY_HANDLE.
 */
static NCRYPT_KEY_HANDLE deserializeKeyHandle(JNIEnv* env, jbyteArray keyHandle) {
    jsize len = env->GetArrayLength(keyHandle);
    if (len != sizeof(NCRYPT_KEY_HANDLE)) {
        return 0;
    }
    NCRYPT_KEY_HANDLE handle = 0;
    env->GetByteArrayRegion(keyHandle, 0, len, reinterpret_cast<jbyte*>(&handle));
    return handle;
}

/**
 * Serializes a NCRYPT_KEY_HANDLE to a Java byte array for passing as opaque handle.
 */
static jbyteArray serializeKeyHandle(JNIEnv* env, NCRYPT_KEY_HANDLE handle) {
    jbyteArray result = env->NewByteArray(sizeof(NCRYPT_KEY_HANDLE));
    env->SetByteArrayRegion(result, 0, sizeof(NCRYPT_KEY_HANDLE),
                             reinterpret_cast<const jbyte*>(&handle));
    return result;
}

/**
 * Throws a Java MsiV2Exception from native code.
 */
static void throwMsiV2Exception(JNIEnv* env, const char* message, const char* errorCode) {
    jclass exClass = env->FindClass("com/microsoft/aad/msal4j/MsiV2Exception");
    if (exClass != nullptr) {
        // Find constructor: MsiV2Exception(String message, String errorCode)
        jmethodID ctor = env->GetMethodID(exClass, "<init>",
                                           "(Ljava/lang/String;Ljava/lang/String;)V");
        if (ctor != nullptr) {
            jstring jMessage = env->NewStringUTF(message);
            jstring jErrorCode = env->NewStringUTF(errorCode);
            jobject ex = env->NewObject(exClass, ctor, jMessage, jErrorCode);
            env->Throw(static_cast<jthrowable>(ex));
            return;
        }
    }
    // Fallback: throw RuntimeException
    jclass rtClass = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(rtClass, message);
}

// ============================================================================
// JNI Implementations
// ============================================================================

JNIEXPORT jbyteArray JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_createKeyGuardRsaKeyNative(
    JNIEnv* env, jclass /*clazz*/, jstring keyName, jint keySizeBits)
{
    NCRYPT_PROV_HANDLE hProvider = 0;
    NCRYPT_KEY_HANDLE hKey = 0;
    SECURITY_STATUS status;

    // Open the Microsoft Software Key Storage Provider
    status = NCryptOpenStorageProvider(&hProvider, MSAL_KEYGUARD_PROVIDER, 0);
    if (FAILED(status)) {
        throwMsiV2Exception(env,
            "[MSI v2] Failed to open NCrypt storage provider. Ensure Windows CNG is available.",
            "msi_v2_keyguard_unavailable");
        return nullptr;
    }

    // Get the key name as a wide string
    const jchar* keyNameChars = env->GetStringChars(keyName, nullptr);
    std::wstring keyNameW(reinterpret_cast<const wchar_t*>(keyNameChars));
    env->ReleaseStringChars(keyName, keyNameChars);

    // Delete any existing key with this name (per-boot key - always fresh).
    // First, try to open an existing key with this name and delete it.
    NCRYPT_KEY_HANDLE hExistingKey = 0;
    SECURITY_STATUS deleteStatus = NCryptOpenKey(hProvider, &hExistingKey,
                                                   keyNameW.c_str(), AT_KEYEXCHANGE, 0);
    if (SUCCEEDED(deleteStatus) && hExistingKey != 0) {
        NCryptDeleteKey(hExistingKey, 0); // hExistingKey is freed by NCryptDeleteKey
    }

    // Create a new persisted key
    status = NCryptCreatePersistedKey(hProvider, &hKey,
                                       NCRYPT_RSA_ALGORITHM_GROUP,
                                       keyNameW.c_str(),
                                       AT_KEYEXCHANGE, 0);
    if (FAILED(status)) {
        NCryptFreeObject(hProvider);
        throwMsiV2Exception(env,
            "[MSI v2] Failed to create NCrypt persisted key. VBS may not be available.",
            "msi_v2_keyguard_unavailable");
        return nullptr;
    }

    // Set key size
    DWORD keySize = static_cast<DWORD>(keySizeBits);
    status = NCryptSetProperty(hKey, NCRYPT_LENGTH_PROPERTY,
                                reinterpret_cast<PBYTE>(&keySize), sizeof(DWORD), 0);
    if (FAILED(status)) {
        NCryptFreeObject(hKey);
        NCryptFreeObject(hProvider);
        throwMsiV2Exception(env,
            "[MSI v2] Failed to set key size property.",
            "msi_v2_error");
        return nullptr;
    }

    // Set VBS (Virtual Isolation) and per-boot flags for KeyGuard protection
    DWORD keyUsagePolicy = NCRYPT_USE_VIRTUAL_ISOLATION_FLAG | NCRYPT_USE_PER_BOOT_KEY_FLAG;
    status = NCryptSetProperty(hKey, NCRYPT_KEY_USAGE_POLICY_PROPERTY,
                                reinterpret_cast<PBYTE>(&keyUsagePolicy), sizeof(DWORD), 0);
    if (FAILED(status)) {
        NCryptFreeObject(hKey);
        NCryptFreeObject(hProvider);
        throwMsiV2Exception(env,
            "[MSI v2] Failed to set VBS KeyGuard properties. "
            "Virtualization Based Security (VBS) must be enabled.",
            "msi_v2_keyguard_unavailable");
        return nullptr;
    }

    // Finalize the key
    status = NCryptFinalizeKey(hKey, NCRYPT_WRITE_KEY_TO_LEGACY_STORE_FLAG);
    if (FAILED(status)) {
        NCryptFreeObject(hKey);
        NCryptFreeObject(hProvider);
        throwMsiV2Exception(env,
            "[MSI v2] Failed to finalize KeyGuard key. VBS attestation may not be available.",
            "msi_v2_keyguard_unavailable");
        return nullptr;
    }

    NCryptFreeObject(hProvider);

    // Return the key handle as an opaque byte array
    return serializeKeyHandle(env, hKey);
}

JNIEXPORT jbyteArray JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_getPublicKeyNative(
    JNIEnv* env, jclass /*clazz*/, jbyteArray keyHandle)
{
    NCRYPT_KEY_HANDLE hKey = deserializeKeyHandle(env, keyHandle);
    if (hKey == 0) {
        throwMsiV2Exception(env, "[MSI v2] Invalid key handle.", "msi_v2_error");
        return nullptr;
    }

    // Export public key in BCRYPT_RSAPUBLIC_BLOB format
    DWORD cbPublicKey = 0;
    SECURITY_STATUS status = NCryptExportKey(hKey, 0, BCRYPT_RSAPUBLIC_BLOB,
                                              nullptr, nullptr, 0, &cbPublicKey, 0);
    if (FAILED(status) || cbPublicKey == 0) {
        throwMsiV2Exception(env, "[MSI v2] Failed to get public key size.", "msi_v2_error");
        return nullptr;
    }

    std::vector<BYTE> publicKeyBlob(cbPublicKey);
    status = NCryptExportKey(hKey, 0, BCRYPT_RSAPUBLIC_BLOB,
                              nullptr, publicKeyBlob.data(), cbPublicKey, &cbPublicKey, 0);
    if (FAILED(status)) {
        throwMsiV2Exception(env, "[MSI v2] Failed to export public key.", "msi_v2_error");
        return nullptr;
    }

    // Convert the BCRYPT_RSAKEY_BLOB to DER SubjectPublicKeyInfo (X.509 format).
    // The blob contains the RSA key parameters; we need to construct a CERT_PUBLIC_KEY_INFO
    // and encode it as DER ASN.1 using CryptEncodeObjectEx.
    //
    // NOTE: Full implementation requires constructing the CERT_PUBLIC_KEY_INFO from
    // the BCRYPT_RSAKEY_BLOB, setting the algorithm OID to szOID_RSA_RSA
    // (1.2.840.113549.1.1.1), and encoding with X509_PUBLIC_KEY_INFO.
    // This stub returns an empty placeholder; the production implementation
    // should use CryptImportPublicKeyInfoEx2 or BCryptExportKey with X509_PUBLIC_KEY_INFO.

    // TODO: Implement full SubjectPublicKeyInfo DER encoding for production use.
    throwMsiV2Exception(env,
        "[MSI v2] getPublicKeyNative: SubjectPublicKeyInfo encoding is not yet fully implemented.",
        "msi_v2_error");
    NCryptFreeObject(hProvider);
    return nullptr;
}

JNIEXPORT jbyteArray JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_signWithKeyGuardNative(
    JNIEnv* env, jclass /*clazz*/, jbyteArray keyHandle, jbyteArray dataToSign)
{
    NCRYPT_KEY_HANDLE hKey = deserializeKeyHandle(env, keyHandle);
    if (hKey == 0) {
        throwMsiV2Exception(env, "[MSI v2] Invalid key handle for signing.", "msi_v2_error");
        return nullptr;
    }

    // Get input data
    jsize dataLen = env->GetArrayLength(dataToSign);
    std::vector<BYTE> data(dataLen);
    env->GetByteArrayRegion(dataToSign, 0, dataLen, reinterpret_cast<jbyte*>(data.data()));

    // Hash the data with SHA-256
    BCRYPT_HASH_HANDLE hHash = nullptr;
    BCRYPT_ALG_HANDLE hHashAlg = nullptr;
    BCryptOpenAlgorithmProvider(&hHashAlg, BCRYPT_SHA256_ALGORITHM, nullptr, 0);
    BCryptCreateHash(hHashAlg, &hHash, nullptr, 0, nullptr, 0, 0);
    BCryptHashData(hHash, data.data(), static_cast<ULONG>(data.size()), 0);
    std::vector<BYTE> digest(32);
    BCryptFinishHash(hHash, digest.data(), 32, 0);
    BCryptDestroyHash(hHash);
    BCryptCloseAlgorithmProvider(hHashAlg, 0);

    // Sign with RSA-PSS/SHA-256
    BCRYPT_PSS_PADDING_INFO paddingInfo = {BCRYPT_SHA256_ALGORITHM, 32};
    DWORD cbSignature = 0;
    SECURITY_STATUS status = NCryptSignHash(hKey,
                                             &paddingInfo,
                                             digest.data(), static_cast<DWORD>(digest.size()),
                                             nullptr, 0, &cbSignature,
                                             BCRYPT_PAD_PSS);
    if (FAILED(status) || cbSignature == 0) {
        throwMsiV2Exception(env, "[MSI v2] Failed to compute signature size.", "msi_v2_error");
        return nullptr;
    }

    std::vector<BYTE> signature(cbSignature);
    status = NCryptSignHash(hKey,
                             &paddingInfo,
                             digest.data(), static_cast<DWORD>(digest.size()),
                             signature.data(), cbSignature, &cbSignature,
                             BCRYPT_PAD_PSS);
    if (FAILED(status)) {
        throwMsiV2Exception(env, "[MSI v2] Failed to sign data with KeyGuard key.", "msi_v2_error");
        return nullptr;
    }

    jbyteArray result = env->NewByteArray(static_cast<jsize>(cbSignature));
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(cbSignature),
                             reinterpret_cast<const jbyte*>(signature.data()));
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_getAttestationTokenNative(
    JNIEnv* env, jclass /*clazz*/, jstring attestationEndpoint, jbyteArray keyHandle)
{
    // NOTE: Full implementation requires AttestationClientLib.dll.
    // This stub outlines the expected call pattern.
    //
    // NCRYPT_KEY_HANDLE hKey = deserializeKeyHandle(env, keyHandle);
    // AttestationClient client;
    // std::string jwt = client.GetAttestationToken(
    //     jstring_to_wstring(env, attestationEndpoint), hKey);
    // return env->NewStringUTF(jwt.c_str());

    throwMsiV2Exception(env,
        "[MSI v2] AttestationClientLib.dll is not available. "
        "This functionality requires the Windows Attestation Client Library.",
        "msi_v2_error");
    return nullptr;
}

JNIEXPORT jstring JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_acquireMtlsTokenNative(
    JNIEnv* env, jclass /*clazz*/,
    jbyteArray keyHandle, jbyteArray certDer,
    jstring tokenEndpointUrl, jstring requestBody)
{
    // NOTE: Full implementation uses WinHTTP with client certificate authentication.
    // The KeyGuard private key is bound to the X.509 certificate for TLS client auth.
    //
    // NCRYPT_KEY_HANDLE hKey = deserializeKeyHandle(env, keyHandle);
    // PCCERT_CONTEXT pCert = /* create from certDer bytes */;
    // CertSetCertificateContextProperty(pCert, CERT_NCRYPT_KEY_HANDLE_PROP_ID, 0, &hKey);
    //
    // HINTERNET hSession = WinHttpOpen(...);
    // HINTERNET hRequest = WinHttpOpenRequest(...);
    // WinHttpSetOption(hRequest, WINHTTP_OPTION_CLIENT_CERT_CONTEXT, pCert, ...);
    // WinHttpSendRequest(...);
    // /* read response body */
    // return env->NewStringUTF(responseBody.c_str());

    throwMsiV2Exception(env,
        "[MSI v2] mTLS token acquisition is not yet implemented in native code.",
        "msi_v2_error");
    return nullptr;
}

JNIEXPORT void JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_freeKeyHandleNative(
    JNIEnv* env, jclass /*clazz*/, jbyteArray keyHandle)
{
    NCRYPT_KEY_HANDLE hKey = deserializeKeyHandle(env, keyHandle);
    if (hKey != 0) {
        NCryptFreeObject(hKey);
    }
}
