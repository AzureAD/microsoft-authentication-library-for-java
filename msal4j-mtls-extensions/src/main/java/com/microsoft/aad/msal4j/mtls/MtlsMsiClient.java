// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Acquires mTLS Proof-of-Possession tokens for Azure Managed Identity by spawning
 * {@code MsalMtlsMsiHelper.exe} as a child process.
 *
 * <h2>Why a subprocess?</h2>
 * <p>Java's TLS stack (JSSE) uses Windows CryptoAPI (CAPI) via {@code SunMSCAPI}, which is the
 * legacy API — not CNG ({@code NCrypt*}). Azure Managed Identity mTLS PoP requires KeyGuard keys,
 * which are created with {@code NCryptCreatePersistedKey} using CNG-only VBS isolation flags
 * ({@code NCRYPT_VBS_KEYISOLATION_FLAG}). CAPI cannot create or use these keys, and JSSE cannot
 * delegate a TLS handshake to a {@code NCRYPT_KEY_HANDLE} directly (unlike .NET's Schannel).
 * Therefore, the entire flow — key creation, CSR, optional MAA attestation, IMDS credential
 * issuance, and the mTLS token request — is delegated to a .NET 8 subprocess that calls
 * {@code Microsoft.Identity.Client}.</p>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>Windows Azure VM with Managed Identity enabled</li>
 *   <li>.NET 8 runtime installed (pre-installed on most Azure Windows VM images)</li>
 *   <li>{@code MsalMtlsMsiHelper.exe} bundled in the JAR or pointed to via
 *       {@code MSAL_MTLS_HELPER_PATH}</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MtlsMsiClient client = new MtlsMsiClient();
 * MtlsMsiHelperResult result = client.acquireToken(
 *     "https://management.azure.com",   // resource
 *     "SystemAssigned",                  // identityType
 *     null,                              // identityId (null for system-assigned)
 *     true,                              // withAttestation
 *     UUID.randomUUID().toString()       // correlationId (optional)
 * );
 * String accessToken = result.getAccessToken();
 * }</pre>
 */
public class MtlsMsiClient {

    private final MtlsMsiHelperLocator locator;

    /** Creates a client using the default {@link MtlsMsiHelperLocator}. */
    public MtlsMsiClient() {
        this(new MtlsMsiHelperLocator());
    }

    /**
     * Creates a client with a custom locator (useful for testing).
     *
     * @param locator custom locator for the helper binary
     */
    public MtlsMsiClient(MtlsMsiHelperLocator locator) {
        this.locator = locator;
    }

    /**
     * Acquires an mTLS PoP token for a Managed Identity resource.
     *
     * @param resource        Azure resource URI (e.g. {@code https://management.azure.com})
     * @param identityType    {@code "SystemAssigned"} or {@code "UserAssigned"}
     * @param identityId      Client ID or resource ID for UserAssigned; {@code null} for SystemAssigned
     * @param withAttestation Whether to include KeyGuard attestation (MAA JWT) in the request
     * @param correlationId   Optional GUID for telemetry correlation; may be {@code null}
     * @return the token result
     * @throws MtlsMsiException if the subprocess fails, the binary cannot be located, or the
     *                           response cannot be parsed
     */
    public MtlsMsiHelperResult acquireToken(
            String resource,
            String identityType,
            String identityId,
            boolean withAttestation,
            String correlationId) throws MtlsMsiException {

        if (resource == null || resource.isEmpty()) {
            throw new MtlsMsiException("resource must not be null or empty");
        }
        if (identityType == null || identityType.isEmpty()) {
            identityType = "SystemAssigned";
        }

        List<String> cmd = buildAcquireTokenCommand(
                resource, identityType, identityId, withAttestation, correlationId);

        String stdout = runProcess(cmd);
        return parseTokenResponse(stdout);
    }

    /**
     * Makes a downstream HTTP call over mutual TLS using the KeyGuard-bound certificate
     * and an mTLS PoP access token.
     *
     * <p><strong>Important:</strong> The downstream server <em>must</em> be configured for
     * required mutual TLS — it must send a TLS {@code CertificateRequest} during the handshake.
     * Public Azure APIs (e.g. Graph, Key Vault) use optional mTLS and will <em>NOT</em> trigger
     * client certificate presentation. Use this mode only with servers explicitly configured to
     * require a client certificate.</p>
     *
     * @param url             The full URL to call
     * @param method          HTTP method ({@code GET}, {@code POST}, etc.)
     * @param token           The mTLS PoP access token to send as the Authorization header
     * @param body            Request body (may be {@code null})
     * @param contentType     Content-Type header (defaults to {@code application/json} if null)
     * @param extraHeaders    Extra headers in {@code "Name: Value"} format (may be null or empty)
     * @param resource        Azure resource URI (used to re-acquire the binding cert)
     * @param identityType    {@code "SystemAssigned"} or {@code "UserAssigned"}
     * @param identityId      Client ID or resource ID for UserAssigned; {@code null} for SystemAssigned
     * @param withAttestation Whether to include attestation when re-acquiring the binding cert
     * @param correlationId   Optional GUID for telemetry; may be {@code null}
     * @param allowInsecureTls Skip server TLS cert validation (self-signed certs in local testing ONLY)
     * @return the HTTP response from the downstream server
     * @throws MtlsMsiException if the subprocess fails
     */
    public MtlsMsiHttpResponse httpRequest(
            String url,
            String method,
            String token,
            String body,
            String contentType,
            List<String> extraHeaders,
            String resource,
            String identityType,
            String identityId,
            boolean withAttestation,
            String correlationId,
            boolean allowInsecureTls) throws MtlsMsiException {

        List<String> cmd = buildHttpRequestCommand(
                url, method, token, body, contentType, extraHeaders,
                resource, identityType, identityId, withAttestation, correlationId, allowInsecureTls);

        String stdout = runProcess(cmd);
        return parseHttpResponse(stdout);
    }

    // ─── Command builders ────────────────────────────────────────────────────

    private List<String> buildAcquireTokenCommand(
            String resource, String identityType, String identityId,
            boolean withAttestation, String correlationId) throws MtlsMsiException {

        List<String> cmd = new ArrayList<>();
        cmd.add(locator.locate());
        cmd.add("--resource");
        cmd.add(resource);
        cmd.add("--identity-type");
        cmd.add(identityType);
        if (identityId != null && !identityId.isEmpty()) {
            cmd.add("--identity-id");
            cmd.add(identityId);
        }
        if (withAttestation) {
            cmd.add("--with-attestation");
        }
        if (correlationId != null && !correlationId.isEmpty()) {
            cmd.add("--correlation-id");
            cmd.add(correlationId);
        }
        return cmd;
    }

    private List<String> buildHttpRequestCommand(
            String url, String method, String token, String body, String contentType,
            List<String> extraHeaders, String resource, String identityType, String identityId,
            boolean withAttestation, String correlationId, boolean allowInsecureTls)
            throws MtlsMsiException {

        List<String> cmd = new ArrayList<>();
        cmd.add(locator.locate());
        cmd.add("--mode");
        cmd.add("http-request");
        cmd.add("--url");
        cmd.add(url);
        cmd.add("--method");
        cmd.add(method != null ? method : "GET");
        cmd.add("--token");
        cmd.add(token);
        if (body != null && !body.isEmpty()) {
            cmd.add("--body");
            cmd.add(body);
        }
        if (contentType != null && !contentType.isEmpty()) {
            cmd.add("--content-type");
            cmd.add(contentType);
        }
        if (extraHeaders != null) {
            for (String h : extraHeaders) {
                cmd.add("--header");
                cmd.add(h);
            }
        }
        if (resource != null && !resource.isEmpty()) {
            cmd.add("--resource");
            cmd.add(resource);
        }
        cmd.add("--identity-type");
        cmd.add(identityType != null ? identityType : "SystemAssigned");
        if (identityId != null && !identityId.isEmpty()) {
            cmd.add("--identity-id");
            cmd.add(identityId);
        }
        if (withAttestation) {
            cmd.add("--with-attestation");
        }
        if (correlationId != null && !correlationId.isEmpty()) {
            cmd.add("--correlation-id");
            cmd.add(correlationId);
        }
        if (allowInsecureTls) {
            cmd.add("--allow-insecure-tls");
        }
        return cmd;
    }

    // ─── Process execution ───────────────────────────────────────────────────

    String runProcess(List<String> cmd) throws MtlsMsiException {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.redirectErrorStream(false);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new MtlsMsiException("Failed to start MsalMtlsMsiHelper process: " + e.getMessage(), e);
        }

        // Read stdout and stderr concurrently to prevent deadlock if either buffer fills.
        final StringBuilder stdoutBuf = new StringBuilder();
        final StringBuilder stderrBuf = new StringBuilder();
        final IOException[] ioError = {null};

        Thread stdoutThread = new Thread(() -> {
            try {
                stdoutBuf.append(readStreamContent(process.getInputStream()));
            } catch (IOException e) {
                ioError[0] = e;
            }
        });
        Thread stderrThread = new Thread(() -> {
            try {
                stderrBuf.append(readStreamContent(process.getErrorStream()));
            } catch (IOException e) {
                // best-effort stderr capture
            }
        });

        stdoutThread.start();
        stderrThread.start();

        int exitCode;
        try {
            stdoutThread.join();
            stderrThread.join();
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            throw new MtlsMsiException("Interrupted while waiting for MsalMtlsMsiHelper: " + e.getMessage(), e);
        }

        if (ioError[0] != null) {
            throw new MtlsMsiException("Error reading MsalMtlsMsiHelper stdout: " + ioError[0].getMessage(), ioError[0]);
        }

        if (exitCode != 0) {
            String errorMsg = parseErrorFromStderr(stderrBuf.toString());
            throw new MtlsMsiException("MsalMtlsMsiHelper exited with code " + exitCode + ": " + errorMsg);
        }

        return stdoutBuf.toString();
    }

    private String readStream(Process process) {
        return "";
    }

    private String readStreamContent(java.io.InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append('\n');
                sb.append(line);
            }
        }
        return sb.toString().trim();
    }

    private String parseErrorFromStderr(String stderr) {
        // Try to extract error_description from the JSON error response
        // {"error":"<code>","error_description":"<msg>"}
        if (stderr == null || stderr.isEmpty()) return "(no stderr)";
        int idx = stderr.indexOf("\"error_description\":");
        if (idx >= 0) {
            int start = stderr.indexOf('"', idx + 20);
            if (start >= 0) {
                int end = stderr.indexOf('"', start + 1);
                if (end > start) {
                    return stderr.substring(start + 1, end);
                }
            }
        }
        return stderr;
    }

    // ─── Response parsers ────────────────────────────────────────────────────

    private MtlsMsiHelperResult parseTokenResponse(String json) throws MtlsMsiException {
        if (json == null || json.isEmpty()) {
            throw new MtlsMsiException("MsalMtlsMsiHelper returned empty output");
        }
        try {
            String accessToken = extractJsonString(json, "access_token");
            String tokenType = extractJsonString(json, "token_type");
            int expiresIn = extractJsonInt(json, "expires_in");
            String bindingCert = extractJsonString(json, "binding_certificate");
            String tenantId = extractJsonString(json, "tenant_id");
            String clientId = extractJsonString(json, "client_id");

            if (accessToken == null || accessToken.isEmpty()) {
                throw new MtlsMsiException("MsalMtlsMsiHelper response missing access_token: " + json);
            }
            return new MtlsMsiHelperResult(accessToken, tokenType, expiresIn, bindingCert, tenantId, clientId);
        } catch (MtlsMsiException e) {
            throw e;
        } catch (Exception e) {
            throw new MtlsMsiException("Failed to parse MsalMtlsMsiHelper token response: " + e.getMessage() + " | response: " + json, e);
        }
    }

    /**
     * Parses the JSON HTTP response from the {@code http-request} subprocess mode.
     */
    public MtlsMsiHttpResponse parseHttpResponse(String json) throws MtlsMsiException {
        if (json == null || json.isEmpty()) {
            throw new MtlsMsiException("MsalMtlsMsiHelper returned empty output for http-request mode");
        }
        try {
            int status = extractJsonInt(json, "status");
            String body = extractJsonString(json, "body");
            return new MtlsMsiHttpResponse(status, body, json);
        } catch (MtlsMsiException e) {
            throw e;
        } catch (Exception e) {
            throw new MtlsMsiException("Failed to parse MsalMtlsMsiHelper http-response: " + e.getMessage() + " | response: " + json, e);
        }
    }

    // Minimal JSON field extractors — avoids adding a JSON library dependency.
    // The subprocess output is machine-generated and well-formed; full DOM parsing is not needed.

    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;
        // skip whitespace
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length()) return null;
        if (json.charAt(valueStart) == '"') {
            // string value
            int end = valueStart + 1;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\') { end += 2; continue; }
                if (c == '"') break;
                end++;
            }
            return json.substring(valueStart + 1, end)
                    .replace("\\n", "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
        if (json.charAt(valueStart) == 'n') return null; // null
        return null;
    }

    static int extractJsonInt(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return 0;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return 0;
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length()) return 0;
        int valueEnd = valueStart;
        while (valueEnd < json.length() && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) valueEnd++;
        try {
            return Integer.parseInt(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
