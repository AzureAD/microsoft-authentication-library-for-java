// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

#pragma once

#include <windows.h>
#include <ncrypt.h>
#include <jni.h>
#include <string>
#include <vector>

/**
 * MsalJNIBridge - Native bridge for MSAL Java MSI v2 mTLS PoP support.
 *
 * This module provides JNI implementations for:
 *   - Creating VBS-isolated, per-boot KeyGuard RSA keys via NCrypt
 *   - Signing data with the hardware-protected KeyGuard key (for PKCS#10 CSR)
 *   - Obtaining attestation JWTs via the Windows AttestationClientLib
 *   - Performing mTLS HTTPS requests using the hardware-bound private key
 *
 * Prerequisites:
 *   - Windows with Virtualization Based Security (VBS) enabled
 *   - AttestationClientLib.dll (Windows Attestation Client Library)
 *   - NCrypt.dll (Windows CNG key storage)
 *
 * Build:
 *   This file is intended to be compiled with Visual Studio or MSBuild for Windows x64.
 *   The output DLL (MsalJNIBridge.dll) must be placed in a location accessible by the JVM.
 */

// NCrypt flags for KeyGuard keys (VBS-isolated, per-boot, non-exportable)
#ifndef NCRYPT_USE_VIRTUAL_ISOLATION_FLAG
#define NCRYPT_USE_VIRTUAL_ISOLATION_FLAG    0x00020000
#endif
#ifndef NCRYPT_USE_PER_BOOT_KEY_FLAG
#define NCRYPT_USE_PER_BOOT_KEY_FLAG         0x00040000
#endif

#define MSAL_KEYGUARD_PROVIDER L"Microsoft Software Key Storage Provider"
#define MSAL_KEYGUARD_ALG      BCRYPT_RSA_ALGORITHM
#define MSAL_KEYGUARD_KEY_SIZE 2048

/**
 * Creates a VBS-isolated per-boot RSA key in the NCrypt key store.
 * Returns an opaque key handle (serialized NCRYPT_KEY_HANDLE) as a byte array.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_createKeyGuardRsaKeyNative(
    JNIEnv* env, jclass clazz, jstring keyName, jint keySizeBits);

/**
 * Returns the DER-encoded RSA SubjectPublicKeyInfo for the given key handle.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_getPublicKeyNative(
    JNIEnv* env, jclass clazz, jbyteArray keyHandle);

/**
 * Signs the given data bytes with the KeyGuard RSA key using RSA-PSS/SHA-256.
 * Returns the signature bytes.
 */
JNIEXPORT jbyteArray JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_signWithKeyGuardNative(
    JNIEnv* env, jclass clazz, jbyteArray keyHandle, jbyteArray dataToSign);

/**
 * Obtains an attestation JWT from the Windows AttestationClientLib.
 * Returns the attestation JWT as a Java String.
 */
JNIEXPORT jstring JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_getAttestationTokenNative(
    JNIEnv* env, jclass clazz, jstring attestationEndpoint, jbyteArray keyHandle);

/**
 * Performs an mTLS HTTPS POST request to the token endpoint using the KeyGuard private key.
 * The TLS client authentication uses the provided X.509 certificate and the hardware-bound key.
 * Returns the HTTP response body as a Java String.
 */
JNIEXPORT jstring JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_acquireMtlsTokenNative(
    JNIEnv* env, jclass clazz,
    jbyteArray keyHandle, jbyteArray certDer,
    jstring tokenEndpointUrl, jstring requestBody);

/**
 * Frees the native NCrypt key handle and associated resources.
 */
JNIEXPORT void JNICALL
Java_com_microsoft_aad_msal4j_WindowsKeyGuardJNI_freeKeyHandleNative(
    JNIEnv* env, jclass clazz, jbyteArray keyHandle);
