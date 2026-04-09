// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * HTTP client for the Azure IMDSv2 credential issuance endpoints used in mTLS PoP.
 *
 * <p>Mirrors msal-go's {@code getPlatformMetadata()} and {@code issueCredential()} in
 * {@code imdsv2.go}.</p>
 *
 * <ul>
 *   <li>GET {@code http://169.254.169.254/metadata/identity/getplatformmetadata?cred-api-version=2.0}
 *       → {@link PlatformMetadata}</li>
 *   <li>POST {@code http://169.254.169.254/metadata/identity/issuecredential?cred-api-version=2.0}
 *       body: {@code {"csr":"<base64>","attestation_token":"<jwt>"}}
 *       → {@link CredentialResponse}</li>
 * </ul>
 */
final class ImdsV2Client {

    private static final String PLATFORM_METADATA_URL =
            "http://169.254.169.254/metadata/identity/getplatformmetadata?cred-api-version=2.0";
    private static final String ISSUE_CREDENTIAL_URL  =
            "http://169.254.169.254/metadata/identity/issuecredential?cred-api-version=2.0";

    private static final int CONNECT_TIMEOUT_MS = 5_000;
    private static final int READ_TIMEOUT_MS    = 30_000;

    private ImdsV2Client() {}

    // ─── Response types ───────────────────────────────────────────────────────

    /** Deserialized response from {@code /getplatformmetadata}. */
    static final class PlatformMetadata {
        final String clientId;
        final String tenantId;
        final String vmId;       // from cuId.vmId
        final String vmssId;     // from cuId.vmssId
        final String attestationEndpoint;

        PlatformMetadata(String clientId, String tenantId, String vmId,
                         String vmssId, String attestationEndpoint) {
            this.clientId            = clientId;
            this.tenantId            = tenantId;
            this.vmId                = vmId;
            this.vmssId              = vmssId;
            this.attestationEndpoint = attestationEndpoint;
        }

        /** The cuId string used as the key name suffix (matches msal-go logic). */
        String cuIdString() {
            return (vmId != null && !vmId.isEmpty()) ? vmId : clientId;
        }
    }

    /** Deserialized response from {@code /issuecredential}. */
    static final class CredentialResponse {
        final String certificate;              // base64-encoded DER
        final String mtlsAuthenticationEndpoint;
        final String clientId;
        final String tenantId;
        final String regionalTokenUrl;

        CredentialResponse(String certificate, String mtlsAuthenticationEndpoint,
                           String clientId, String tenantId, String regionalTokenUrl) {
            this.certificate               = certificate;
            this.mtlsAuthenticationEndpoint = mtlsAuthenticationEndpoint;
            this.clientId                  = clientId;
            this.tenantId                  = tenantId;
            this.regionalTokenUrl          = regionalTokenUrl;
        }
    }

    // ─── API ─────────────────────────────────────────────────────────────────

    static PlatformMetadata getPlatformMetadata() throws MtlsMsiException {
        String json = httpGet(PLATFORM_METADATA_URL);
        return parsePlatformMetadata(json);
    }

    static CredentialResponse issueCredential(String csrBase64, String attestationToken)
            throws MtlsMsiException {
        // Build JSON body manually to avoid adding a JSON library dependency.
        StringBuilder body = new StringBuilder("{\"csr\":\"");
        body.append(csrBase64).append("\"");
        if (attestationToken != null && !attestationToken.isEmpty()) {
            body.append(",\"attestation_token\":\"").append(attestationToken).append("\"");
        }
        body.append("}");
        String json = httpPost(ISSUE_CREDENTIAL_URL, body.toString());
        return parseCredentialResponse(json);
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private static String httpGet(String urlStr) throws MtlsMsiException {
        try {
            HttpURLConnection conn = openConnection(urlStr);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Metadata", "true");
            conn.setRequestProperty("x-ms-client-request-id", UUID.randomUUID().toString());
            return readResponse(conn, urlStr);
        } catch (IOException e) {
            throw new MtlsMsiException("IMDS GET " + urlStr + " failed: " + e.getMessage(), e);
        }
    }

    private static String httpPost(String urlStr, String jsonBody) throws MtlsMsiException {
        try {
            HttpURLConnection conn = openConnection(urlStr);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Metadata", "true");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("x-ms-client-request-id", UUID.randomUUID().toString());
            conn.setDoOutput(true);
            byte[] bodyBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }
            return readResponse(conn, urlStr);
        } catch (IOException e) {
            throw new MtlsMsiException("IMDS POST " + urlStr + " failed: " + e.getMessage(), e);
        }
    }

    private static HttpURLConnection openConnection(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        return conn;
    }

    private static String readResponse(HttpURLConnection conn, String urlStr)
            throws IOException, MtlsMsiException {
        int status = conn.getResponseCode();
        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body;
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            body = sb.toString();
        }
        if (status != 200) {
            throw new MtlsMsiException(
                    "IMDS " + urlStr + " returned HTTP " + status + ": " + body);
        }
        return body;
    }

    // ─── JSON parsers (no external library) ───────────────────────────────────

    private static PlatformMetadata parsePlatformMetadata(String json) throws MtlsMsiException {
        String clientId            = extractString(json, "clientId");
        String tenantId            = extractString(json, "tenantId");
        String attestationEndpoint = extractString(json, "attestationEndpoint");

        // cuId is a nested object: {"vmId":"...","vmssId":"..."}
        String vmId   = null;
        String vmssId = null;
        int cuIdIdx = json.indexOf("\"cuId\"");
        if (cuIdIdx >= 0) {
            int objStart = json.indexOf('{', cuIdIdx);
            int objEnd   = json.indexOf('}', objStart);
            if (objStart >= 0 && objEnd > objStart) {
                String cuIdObj = json.substring(objStart, objEnd + 1);
                vmId   = extractString(cuIdObj, "vmId");
                vmssId = extractString(cuIdObj, "vmssId");
            }
        }

        if (clientId == null || clientId.isEmpty()) {
            throw new MtlsMsiException(
                    "IMDS /getplatformmetadata returned empty clientId. " +
                    "Ensure Managed Identity is enabled on this VM.");
        }
        if (tenantId == null || tenantId.isEmpty()) {
            throw new MtlsMsiException(
                    "IMDS /getplatformmetadata returned empty tenantId.");
        }

        return new PlatformMetadata(clientId, tenantId, vmId, vmssId, attestationEndpoint);
    }

    private static CredentialResponse parseCredentialResponse(String json) throws MtlsMsiException {
        String certificate               = extractString(json, "certificate");
        String mtlsAuthenticationEndpoint = extractString(json, "mtls_authentication_endpoint");
        String clientId                  = extractString(json, "client_id");
        String tenantId                  = extractString(json, "tenant_id");
        String regionalTokenUrl          = extractString(json, "regional_token_url");

        if (certificate == null || certificate.isEmpty()) {
            throw new MtlsMsiException(
                    "IMDS /issuecredential returned empty certificate: " + json);
        }

        return new CredentialResponse(certificate, mtlsAuthenticationEndpoint,
                clientId, tenantId, regionalTokenUrl);
    }

    /**
     * Minimal JSON string extractor. Handles the well-formed JSON output from IMDS.
     * Returns null if the key is not present or its value is not a JSON string.
     *
     * <p>Escape sequences are processed sequentially (one pass) to avoid incorrect
     * behaviour when {@code \\} (escaped backslash) is followed by {@code t}, {@code n}, etc.</p>
     */
    static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;

        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) {
            valueStart++;
        }
        if (valueStart >= json.length() || json.charAt(valueStart) != '"') return null;

        // Build the unescaped value with a single sequential pass.
        StringBuilder sb = new StringBuilder();
        int i = valueStart + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                switch (next) {
                    case '"':  sb.append('"');  i += 2; continue;
                    case '\\': sb.append('\\'); i += 2; continue;
                    case 'n':  sb.append('\n'); i += 2; continue;
                    case 'r':  sb.append('\r'); i += 2; continue;
                    case 't':  sb.append('\t'); i += 2; continue;
                    default:   sb.append(c);   i++;    continue;
                }
            }
            if (c == '"') break;
            sb.append(c);
            i++;
        }
        return sb.toString();
    }
}
