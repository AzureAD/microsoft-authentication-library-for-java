// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the MSI v2 mTLS Proof-of-Possession (PoP) token acquisition flow using
 * Windows KeyGuard attestation.
 * <p>
 * The flow consists of 7 steps:
 * <ol>
 *   <li>Get platform metadata from IMDS ({@code /metadata/identity/getPlatformMetadata})</li>
 *   <li>Create a VBS-protected KeyGuard RSA key via JNI</li>
 *   <li>Build a PKCS#10 CSR signed with the KeyGuard key, including the cuId OID attribute</li>
 *   <li>Obtain an attestation JWT from the Windows AttestationClientLib via JNI</li>
 *   <li>Issue a short-lived mTLS credential from IMDS ({@code /metadata/identity/issuecredential})</li>
 *   <li>Parse the issued X.509 certificate from the IMDS response</li>
 *   <li>Acquire an {@code mtls_pop} token from the regional ESTS endpoint using mTLS</li>
 * </ol>
 * <p>
 * <b>No silent fallback:</b> if MSI v2 is explicitly requested and fails at any step,
 * a {@link MsiV2Exception} is thrown and MSI v1 is NOT attempted as a fallback.
 * <p>
 * <b>Platform requirement:</b> Windows with Virtualization Based Security (VBS) enabled.
 */
class MsiV2 {

    private static final Logger LOG = LoggerFactory.getLogger(MsiV2.class);

    static final String IMDS_BASE_URL = "http://169.254.169.254";
    static final String PLATFORM_METADATA_PATH = "/metadata/identity/getPlatformMetadata";
    static final String ISSUECREDENTIAL_PATH = "/metadata/identity/issuecredential";
    static final String IMDS_API_VERSION = "2018-02-01";
    static final String KEYGUARD_KEY_NAME = "MsalKeyGuardKey";
    static final int KEYGUARD_RSA_KEY_SIZE = 2048;
    static final String MTLS_TOKEN_TYPE = "mtls_pop";
    static final String OAUTH2_GRANT_TYPE = "client_credentials";

    /**
     * Executes the full MSI v2 7-step flow to obtain an mTLS PoP token.
     *
     * @param msalRequest   the current MSAL request (provides HTTP helper and request context)
     * @param serviceBundle the service bundle with HTTP client
     * @param resource      the Azure resource URI to acquire a token for
     * @return a {@link ManagedIdentityResponse} containing the acquired mTLS PoP token
     * @throws MsiV2Exception if any step fails (no fallback to MSI v1)
     */
    static ManagedIdentityResponse obtainToken(MsalRequest msalRequest,
                                                ServiceBundle serviceBundle,
                                                String resource) {
        // Check native library availability
        if (!WindowsKeyGuardJNI.isNativeLibraryLoaded()) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_KEYGUARD_UNAVAILABLE,
                    MsalError.MSI_V2_KEYGUARD_UNAVAILABLE);
        }

        byte[] keyHandle = null;
        try {
            // Step 1: Get platform metadata from IMDS
            LOG.debug("[MSI v2] Step 1: Retrieving platform metadata from IMDS.");
            CsrMetadata metadata = getPlatformMetadata(msalRequest, serviceBundle);

            // Step 2: Create KeyGuard RSA key (VBS-isolated, per-boot, non-exportable)
            LOG.debug("[MSI v2] Step 2: Creating KeyGuard RSA-{} key.", KEYGUARD_RSA_KEY_SIZE);
            keyHandle = createKeyGuardKey();
            byte[] publicKeyDer = WindowsKeyGuardJNI.getPublicKeyNative(keyHandle);

            // Step 3: Build PKCS#10 CSR with Microsoft cuId OID attribute
            LOG.debug("[MSI v2] Step 3: Generating PKCS#10 CSR with cuId attribute.");
            String csrPem = CsrGenerator.generate(publicKeyDer, metadata.cuId, keyHandle);
            String csrBase64 = extractBase64FromPem(csrPem);

            // Step 4: Obtain attestation JWT from AttestationClientLib
            LOG.debug("[MSI v2] Step 4: Obtaining attestation JWT from {}.",
                    metadata.attestationEndpoint);
            String attestationToken = getAttestationToken(metadata.attestationEndpoint, keyHandle);

            // Step 5: Issue mTLS credential from IMDS
            LOG.debug("[MSI v2] Step 5: Issuing mTLS credential from IMDS.");
            IssueCertificateResponse certResponse = issueCredential(
                    msalRequest, serviceBundle, csrBase64, attestationToken);

            // Step 6: Parse the issued X.509 certificate
            LOG.debug("[MSI v2] Step 6: Parsing issued X.509 certificate.");
            byte[] certDer = Base64.getDecoder().decode(certResponse.certificate);

            // Step 7: Acquire mTLS PoP token from regional ESTS endpoint
            LOG.debug("[MSI v2] Step 7: Acquiring mTLS PoP token from {}.",
                    certResponse.mtlsAuthenticationEndpoint);
            return acquireMtlsToken(keyHandle, certDer, certResponse, resource);

        } finally {
            // Free the native key handle to avoid resource leaks
            if (keyHandle != null) {
                try {
                    WindowsKeyGuardJNI.freeKeyHandleNative(keyHandle);
                } catch (Exception e) {
                    LOG.warn("[MSI v2] Failed to free native key handle: {}", e.getMessage());
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 1: Get platform metadata
    // -------------------------------------------------------------------------

    static CsrMetadata getPlatformMetadata(MsalRequest msalRequest, ServiceBundle serviceBundle) {
        String url = IMDS_BASE_URL + PLATFORM_METADATA_PATH + "?api-version=" + IMDS_API_VERSION;
        Map<String, String> headers = new HashMap<>();
        headers.put("Metadata", "true");

        HttpRequest request = new HttpRequest(HttpMethod.GET, url, headers);
        IHttpResponse response;
        try {
            response = serviceBundle.getHttpHelper().executeHttpRequest(
                    request, msalRequest.requestContext(), serviceBundle);
        } catch (Exception e) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_PLATFORM_METADATA_FAILED + " " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }

        if (response.statusCode() != 200) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_PLATFORM_METADATA_FAILED
                            + " HTTP " + response.statusCode() + ": " + response.body(),
                    MsalError.MSI_V2_ERROR);
        }

        try (JsonReader reader = JsonProviders.createReader(response.body())) {
            return CsrMetadata.fromJson(reader);
        } catch (IOException e) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_PLATFORM_METADATA_FAILED + " JSON parse error: " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 2: Create KeyGuard RSA key
    // -------------------------------------------------------------------------

    private static byte[] createKeyGuardKey() {
        try {
            byte[] keyHandle = WindowsKeyGuardJNI.createKeyGuardRsaKeyNative(
                    KEYGUARD_KEY_NAME, KEYGUARD_RSA_KEY_SIZE);
            if (keyHandle == null || keyHandle.length == 0) {
                throw new MsiV2Exception(
                        MsalErrorMessage.MSI_V2_KEYGUARD_UNAVAILABLE,
                        MsalError.MSI_V2_KEYGUARD_UNAVAILABLE);
            }
            return keyHandle;
        } catch (MsiV2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_KEYGUARD_UNAVAILABLE + " " + e.getMessage(),
                    MsalError.MSI_V2_KEYGUARD_UNAVAILABLE, e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 4: Get attestation token
    // -------------------------------------------------------------------------

    private static String getAttestationToken(String attestationEndpoint, byte[] keyHandle) {
        try {
            String token = WindowsKeyGuardJNI.getAttestationTokenNative(attestationEndpoint, keyHandle);
            if (StringHelper.isNullOrBlank(token)) {
                throw new MsiV2Exception(
                        "[MSI v2] Attestation service returned an empty token.",
                        MsalError.MSI_V2_ERROR);
            }
            return token;
        } catch (MsiV2Exception e) {
            throw e;
        } catch (Exception e) {
            throw new MsiV2Exception(
                    "[MSI v2] Attestation failed: " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 5: Issue mTLS credential from IMDS
    // -------------------------------------------------------------------------

    static IssueCertificateResponse issueCredential(MsalRequest msalRequest,
                                                     ServiceBundle serviceBundle,
                                                     String csrBase64,
                                                     String attestationToken) {
        String url = IMDS_BASE_URL + ISSUECREDENTIAL_PATH + "?api-version=" + IMDS_API_VERSION;
        Map<String, String> headers = new HashMap<>();
        headers.put("Metadata", "true");
        headers.put("Content-Type", "application/json");

        // Build JSON request body
        String body = buildIssueCertificateRequestBody(csrBase64, attestationToken);

        HttpRequest request = new HttpRequest(HttpMethod.POST, url, headers, body);
        IHttpResponse response;
        try {
            response = serviceBundle.getHttpHelper().executeHttpRequest(
                    request, msalRequest.requestContext(), serviceBundle);
        } catch (Exception e) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_ISSUECREDENTIAL_FAILED + " " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }

        if (response.statusCode() != 200) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_ISSUECREDENTIAL_FAILED
                            + " HTTP " + response.statusCode() + ": " + response.body(),
                    MsalError.MSI_V2_ERROR);
        }

        try (JsonReader reader = JsonProviders.createReader(response.body())) {
            return IssueCertificateResponse.fromJson(reader);
        } catch (IOException e) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_ISSUECREDENTIAL_FAILED + " JSON parse error: " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }
    }

    private static String buildIssueCertificateRequestBody(String csrBase64, String attestationToken) {
        // Simple JSON serialization without external library
        return "{\"csr\":\"" + escapeJson(csrBase64) + "\","
                + "\"attestation_token\":\"" + escapeJson(attestationToken) + "\"}";
    }

    // -------------------------------------------------------------------------
    // Step 7: Acquire mTLS PoP token
    // -------------------------------------------------------------------------

    private static ManagedIdentityResponse acquireMtlsToken(byte[] keyHandle,
                                                              byte[] certDer,
                                                              IssueCertificateResponse certResponse,
                                                              String resource) {
        // Build the OAuth2 token request body
        String scope = resource.endsWith("/.default") ? resource : resource + "/.default";
        String requestBody = "grant_type=" + OAUTH2_GRANT_TYPE
                + "&client_id=" + urlEncode(certResponse.clientId)
                + "&scope=" + urlEncode(scope)
                + "&token_type=" + MTLS_TOKEN_TYPE;

        // Build the token endpoint URL: {mtlsEndpoint}/{tenantId}/oauth2/v2.0/token
        String tokenEndpointUrl = buildTokenEndpointUrl(
                certResponse.mtlsAuthenticationEndpoint, certResponse.tenantId);

        // Use the native mTLS method to acquire the token
        String responseBody = WindowsKeyGuardJNI.acquireMtlsTokenNative(
                keyHandle, certDer, tokenEndpointUrl, requestBody);

        return parseMtlsTokenResponse(responseBody, resource);
    }

    static String buildTokenEndpointUrl(String mtlsEndpoint, String tenantId) {
        String base = mtlsEndpoint;
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + tenantId + "/oauth2/v2.0/token";
    }

    private static ManagedIdentityResponse parseMtlsTokenResponse(String responseBody, String resource) {
        try (JsonReader reader = JsonProviders.createReader(responseBody)) {
            ManagedIdentityResponse response = new ManagedIdentityResponse();
            reader.readObject(r -> {
                while (r.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = r.getFieldName();
                    r.nextToken();
                    switch (fieldName) {
                        case "access_token":
                            response.accessToken = r.getString();
                            break;
                        case "expires_in":
                            // Convert expires_in (seconds from now) to expires_on (epoch seconds)
                            long expiresIn = r.getLong();
                            response.expiresOn = String.valueOf(
                                    (System.currentTimeMillis() / 1000) + expiresIn);
                            break;
                        case "expires_on":
                            response.expiresOn = r.getString();
                            break;
                        case "token_type":
                            response.tokenType = r.getString();
                            break;
                        case "client_id":
                            response.clientId = r.getString();
                            break;
                        default:
                            r.skipChildren();
                            break;
                    }
                }
                return response;
            });
            response.resource = resource;
            if (response.tokenType == null) {
                response.tokenType = MTLS_TOKEN_TYPE;
            }
            return response;
        } catch (IOException e) {
            throw new MsiV2Exception(
                    MsalErrorMessage.MSI_V2_TOKEN_ACQUISITION_FAILED + " JSON parse error: " + e.getMessage(),
                    MsalError.MSI_V2_ERROR, e);
        }
    }

    // -------------------------------------------------------------------------
    // Utility helpers
    // -------------------------------------------------------------------------

    private static String extractBase64FromPem(String pem) {
        return pem
                .replace("-----BEGIN CERTIFICATE REQUEST-----", "")
                .replace("-----END CERTIFICATE REQUEST-----", "")
                .replaceAll("\\s", "");
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String urlEncode(String value) {
        if (value == null) {
            return "";
        }
        try {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return value;
        }
    }

    private MsiV2() {
        // Utility class, not instantiable
    }
}
