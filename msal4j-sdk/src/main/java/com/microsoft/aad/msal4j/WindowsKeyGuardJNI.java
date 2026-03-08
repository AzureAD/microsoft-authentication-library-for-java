// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JNI bridge to the Windows KeyGuard native library ({@code MsalJNIBridge.dll}).
 * <p>
 * Provides access to:
 * <ul>
 *   <li>Creating hardware-protected RSA keys via NCrypt and VBS (Virtualization Based Security)</li>
 *   <li>Signing data with the KeyGuard key for PKCS#10 CSR generation</li>
 *   <li>Obtaining attestation JWTs via the Windows AttestationClientLib</li>
 *   <li>Performing mTLS HTTPS connections using the hardware-bound private key</li>
 * </ul>
 * <p>
 * <b>Platform requirements:</b> Windows with Virtualization Based Security (VBS) enabled.
 * All native methods will throw {@link MsiV2Exception} with error code
 * {@link MsalError#MSI_V2_KEYGUARD_UNAVAILABLE} when called on unsupported platforms.
 */
class WindowsKeyGuardJNI {

    private static final Logger LOG = LoggerFactory.getLogger(WindowsKeyGuardJNI.class);

    private static final boolean NATIVE_LIBRARY_LOADED;

    static {
        boolean loaded = false;
        try {
            System.loadLibrary("MsalJNIBridge");
            loaded = true;
            LOG.debug("[MSI v2] Native MsalJNIBridge library loaded successfully.");
        } catch (UnsatisfiedLinkError e) {
            LOG.debug("[MSI v2] Native MsalJNIBridge library is not available: {}", e.getMessage());
        }
        NATIVE_LIBRARY_LOADED = loaded;
    }

    /**
     * Returns {@code true} if the native KeyGuard library is loaded and available.
     * This does not guarantee VBS is enabled; call {@link #createKeyGuardRsaKeyNative} to determine
     * actual availability at runtime.
     *
     * @return {@code true} if the native library was loaded
     */
    static boolean isNativeLibraryLoaded() {
        return NATIVE_LIBRARY_LOADED;
    }

    /**
     * Creates a VBS-isolated, per-boot RSA key in the Windows CNG key store via NCrypt.
     * The key is non-exportable and hardware-protected by Virtualization Based Security (VBS).
     *
     * @param keyName    the NCrypt key name (e.g., {@code "MsalKeyGuardKey"})
     * @param keySizeBits the RSA key size in bits (e.g., {@code 2048})
     * @return an opaque native key handle used for subsequent signing and attestation calls.
     *         The caller is responsible for freeing this handle via {@link #freeKeyHandleNative}.
     * @throws MsiV2Exception if VBS is unavailable or key creation fails
     */
    static native byte[] createKeyGuardRsaKeyNative(String keyName, int keySizeBits);

    /**
     * Returns the DER-encoded RSA public key corresponding to the provided native key handle.
     *
     * @param keyHandle the native key handle returned by {@link #createKeyGuardRsaKeyNative}
     * @return DER-encoded SubjectPublicKeyInfo (RSA public key)
     * @throws MsiV2Exception if the key handle is invalid or the operation fails
     */
    static native byte[] getPublicKeyNative(byte[] keyHandle);

    /**
     * Signs the given data with the KeyGuard RSA private key using RSA-PSS/SHA-256.
     *
     * @param keyHandle  the native key handle returned by {@link #createKeyGuardRsaKeyNative}
     * @param dataToSign the raw bytes to sign (typically the DER-encoded CertificationRequestInfo TBS)
     * @return the RSA-PSS/SHA-256 signature bytes
     * @throws MsiV2Exception if signing fails
     */
    static native byte[] signWithKeyGuardNative(byte[] keyHandle, byte[] dataToSign);

    /**
     * Obtains a JWT attestation token from the Windows AttestationClientLib.
     * The attestation proves that the key is VBS-protected and non-exportable.
     * <p>
     * <b>Note:</b> This method requires {@code AttestationClientLib.dll} to be present.
     * If the DLL is not available, a {@link MsiV2Exception} will be thrown.
     *
     * @param attestationEndpoint the URL of the attestation service (from IMDS platform metadata)
     * @param keyHandle           the native key handle to attest
     * @return a JWT string representing the attestation token
     * @throws MsiV2Exception if attestation fails or the attestation service is unavailable
     */
    static native String getAttestationTokenNative(String attestationEndpoint, byte[] keyHandle);

    /**
     * Performs an mTLS HTTPS POST request to the specified token endpoint using the KeyGuard
     * private key as the client certificate private key.
     * <p>
     * This method handles the TLS handshake natively, using the hardware-protected key for the
     * client authentication step of the mTLS connection.
     * <p>
     * <b>Note:</b> The native implementation requires Windows with VBS enabled and
     * {@code MsalJNIBridge.dll} built and available in the library path.
     *
     * @param keyHandle           the native key handle for the client certificate private key
     * @param certDer             the DER-encoded X.509 client certificate (from IMDS issuecredential)
     * @param tokenEndpointUrl    the mTLS token endpoint URL (e.g., regional ESTS endpoint)
     * @param requestBody         the URL-encoded OAuth2 token request body
     * @return the HTTP response body as a UTF-8 string (JSON token response)
     * @throws MsiV2Exception if the mTLS connection or token request fails
     */
    static native String acquireMtlsTokenNative(byte[] keyHandle, byte[] certDer,
                                                 String tokenEndpointUrl, String requestBody);

    /**
     * Frees the native memory and NCrypt key handle associated with a KeyGuard key.
     * This should be called when the key handle is no longer needed to avoid resource leaks.
     *
     * @param keyHandle the native key handle to free
     */
    static native void freeKeyHandleNative(byte[] keyHandle);

    private WindowsKeyGuardJNI() {
        // Utility class, not instantiable
    }
}
