// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Pointer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * Acquires mTLS Proof-of-Possession tokens for Azure Managed Identity.
 *
 * <p>Uses JNA to call Windows CNG ({@code ncrypt.dll}) and, optionally,
 * {@code AttestationClientLib.dll} directly from the JVM. No .NET runtime or subprocess is
 * required.</p>
 *
 * <h2>Architecture</h2>
 * <ol>
 *   <li>Get or create a KeyGuard CNG key via {@link CngKeyGuard} (JNA → {@code ncrypt.dll}).</li>
 *   <li>Generate a PKCS#10 CSR signed with that key ({@link Pkcs10Builder}).</li>
 *   <li>Optionally obtain an MAA attestation JWT from {@code AttestationClientLib.dll}.</li>
 *   <li>POST CSR (+ attestation JWT) to IMDS {@code /issuecredential} → X.509 certificate.</li>
 *   <li>Build a JSSE {@link SSLContext} backed by a custom {@link CngProvider} that signs the
 *       TLS handshake using {@code NCryptSignHash} — the private key never leaves CNG.</li>
 *   <li>POST to the regional mTLS token endpoint and return the {@code mtls_pop} token.</li>
 * </ol>
 *
 * <h2>Requirements</h2>
 * <ul>
 *   <li>Windows Azure VM with Managed Identity enabled</li>
 *   <li>{@code AttestationClientLib.dll} on {@code PATH} (from the
 *       {@code Microsoft.Azure.Security.KeyGuardAttestation} NuGet package) when
 *       {@code withAttestation=true} and the VM is Trusted Launch</li>
 * </ul>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MtlsMsiClient client = new MtlsMsiClient();
 * MtlsMsiHelperResult result = client.acquireToken(
 *     "https://management.azure.com",   // resource
 *     "SystemAssigned",                  // identityType (informational — IMDS determines identity)
 *     null,                              // identityId (null for system-assigned)
 *     true,                              // withAttestation
 *     UUID.randomUUID().toString()       // correlationId (optional)
 * );
 * String accessToken = result.getAccessToken();
 * }</pre>
 */
public class MtlsMsiClient {

    /** Creates a new client. */
    public MtlsMsiClient() {}

    /**
     * Acquires an mTLS PoP token for a Managed Identity resource.
     *
     * <p>Note: {@code identityType} and {@code identityId} are accepted for API compatibility but
     * are not forwarded to IMDS — the VM's managed identity configuration determines which
     * identity is used. For UserAssigned identities, configure the VM with the desired identity
     * before calling this method.</p>
     *
     * @param resource        Azure resource URI (e.g. {@code https://management.azure.com})
     * @param identityType    Accepted for compatibility; IMDS ignores it in the JNA flow
     * @param identityId      Accepted for compatibility; IMDS ignores it in the JNA flow
     * @param withAttestation Whether to request MAA attestation (requires Trusted Launch VM with
     *                        {@code AttestationClientLib.dll} on PATH)
     * @param correlationId   Optional GUID for telemetry; may be {@code null}
     * @return the mTLS PoP token result
     * @throws MtlsMsiException on key creation, IMDS, or token acquisition failure
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

        MtlsBindingInfo binding = MtlsBindingCertManager.getOrCreate(withAttestation);
        SSLSocketFactory sslFactory = buildSslSocketFactory(binding, false);

        String tokenUrl = buildTokenUrl(binding.mtlsEndpoint, binding.tenantId);
        String requestBody = buildTokenRequestBody(binding.clientId, resource);
        String requestId   = correlationId != null ? correlationId : UUID.randomUUID().toString();

        String responseJson = httpsPost(tokenUrl, requestBody, "application/x-www-form-urlencoded",
                sslFactory, requestId);
        return parseTokenResponse(responseJson, binding);
    }

    /**
     * Makes a downstream HTTP call over mutual TLS using the KeyGuard-bound certificate
     * and an mTLS PoP access token.
     *
     * <p><strong>Important:</strong> The downstream server <em>must</em> be configured for
     * required mutual TLS — it must send a TLS {@code CertificateRequest} during the handshake.
     * Public Azure APIs (Graph, Key Vault, etc.) use optional mTLS and will <em>NOT</em> trigger
     * client certificate presentation. Use this only with servers that require a client cert.</p>
     *
     * @param url             The full URL to call
     * @param method          HTTP method ({@code GET}, {@code POST}, etc.)
     * @param token           The mTLS PoP access token for the Authorization header
     * @param body            Request body (may be {@code null})
     * @param contentType     Content-Type (defaults to {@code application/json} if null)
     * @param extraHeaders    Extra headers in {@code "Name: Value"} format (may be null)
     * @param resource        Azure resource URI (used to resolve binding if not cached)
     * @param identityType    Accepted for compatibility; IMDS ignores it in the JNA flow
     * @param identityId      Accepted for compatibility; IMDS ignores it in the JNA flow
     * @param withAttestation Whether to include attestation when refreshing the binding cert
     * @param correlationId   Optional GUID for telemetry; may be {@code null}
     * @param allowInsecureTls Skip server TLS cert validation (for self-signed certs in testing ONLY)
     * @return the HTTP response from the downstream server
     * @throws MtlsMsiException if the binding cert cannot be acquired or the request fails
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

        MtlsBindingInfo binding = MtlsBindingCertManager.getOrCreate(withAttestation);
        SSLSocketFactory sslFactory = buildSslSocketFactory(binding, allowInsecureTls);

        String requestId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        return httpsRequest(url, method != null ? method : "GET", token, body,
                contentType != null ? contentType : "application/json",
                extraHeaders, sslFactory, requestId);
    }

    // ─── Token request helpers ─────────────────────────────────────────────────

    private static String buildTokenUrl(String mtlsEndpoint, String tenantId) {
        String base = mtlsEndpoint.endsWith("/")
                ? mtlsEndpoint.substring(0, mtlsEndpoint.length() - 1)
                : mtlsEndpoint;
        return base + "/" + tenantId + "/oauth2/v2.0/token";
    }

    private static String buildTokenRequestBody(String clientId, String resource) {
        String scope = resource.endsWith("/.default") ? resource : resource + "/.default";
        return "grant_type=client_credentials"
                + "&client_id=" + urlEncode(clientId)
                + "&scope="     + urlEncode(scope)
                + "&token_type=mtls_pop";
    }

    private static String urlEncode(String s) {
        try {
            return java.net.URLEncoder.encode(s, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            return s;
        }
    }

    // ─── JSSE mTLS helpers ────────────────────────────────────────────────────

    private static SSLSocketFactory buildSslSocketFactory(MtlsBindingInfo binding,
                                                           boolean insecure)
            throws MtlsMsiException {
        CngProvider.installIfAbsent();

        X509KeyManager km = new CngX509KeyManager(binding.privateKey, binding.certificate);
        TrustManager[] tms = insecure ? new TrustManager[]{TRUST_ALL} : null;

        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(new KeyManager[]{km}, tms, null);
            return ctx.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new MtlsMsiException("Failed to build mTLS SSLContext: " + e.getMessage(), e);
        }
    }

    /** X509KeyManager that returns the CNG-backed key and the IMDS certificate. */
    private static final class CngX509KeyManager implements X509KeyManager {
        private final CngRsaPrivateKey key;
        private final X509Certificate  cert;

        CngX509KeyManager(CngRsaPrivateKey key, X509Certificate cert) {
            this.key  = key;
            this.cert = cert;
        }

        @Override public String[] getClientAliases(String keyType, Principal[] issuers) {
            return new String[]{"mtls"};
        }
        @Override public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket s) {
            return "mtls";
        }
        @Override public X509Certificate[] getCertificateChain(String alias) {
            return new X509Certificate[]{cert};
        }
        @Override public PrivateKey getPrivateKey(String alias) { return key; }

        @Override public String[] getServerAliases(String keyType, Principal[] issuers) { return null; }
        @Override public String chooseServerAlias(String keyType, Principal[] issuers, Socket s) { return null; }
    }

    /** Accepts any server certificate — for testing only. */
    private static final TrustManager TRUST_ALL = new X509TrustManager() {
        public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        public void checkClientTrusted(X509Certificate[] c, String a) {}
        public void checkServerTrusted(X509Certificate[] c, String a) {}
    };

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private static String httpsPost(String urlStr, String body, String contentType,
                                     SSLSocketFactory sslFactory, String requestId)
            throws MtlsMsiException {
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
            conn.setSSLSocketFactory(sslFactory);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestProperty("x-ms-client-request-id", requestId);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);
            conn.setDoOutput(true);
            byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
            conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
            try (OutputStream os = conn.getOutputStream()) {
                os.write(bodyBytes);
            }
            return readHttpsResponse(conn, urlStr);
        } catch (IOException e) {
            throw new MtlsMsiException("mTLS POST to " + urlStr + " failed: " + e.getMessage(), e);
        }
    }

    private static MtlsMsiHttpResponse httpsRequest(String urlStr, String method, String token,
                                                     String body, String contentType,
                                                     List<String> extraHeaders,
                                                     SSLSocketFactory sslFactory, String requestId)
            throws MtlsMsiException {
        try {
            HttpsURLConnection conn = (HttpsURLConnection) new URL(urlStr).openConnection();
            conn.setSSLSocketFactory(sslFactory);
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", contentType);
            conn.setRequestProperty("x-ms-client-request-id", requestId);
            conn.setConnectTimeout(10_000);
            conn.setReadTimeout(30_000);

            if (extraHeaders != null) {
                for (String header : extraHeaders) {
                    int colon = header.indexOf(':');
                    if (colon > 0) {
                        conn.setRequestProperty(header.substring(0, colon).trim(),
                                header.substring(colon + 1).trim());
                    }
                }
            }

            if (body != null && !body.isEmpty()) {
                conn.setDoOutput(true);
                byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
                conn.setRequestProperty("Content-Length", String.valueOf(bodyBytes.length));
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bodyBytes);
                }
            }

            int status   = conn.getResponseCode();
            InputStream  stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
            String responseBody = readStream(stream);
            return new MtlsMsiHttpResponse(status, responseBody, responseBody);
        } catch (IOException e) {
            throw new MtlsMsiException("mTLS " + method + " " + urlStr + " failed: " + e.getMessage(), e);
        }
    }

    private static String readHttpsResponse(HttpsURLConnection conn, String urlStr)
            throws IOException, MtlsMsiException {
        int status = conn.getResponseCode();
        InputStream stream = status >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String body = readStream(stream);
        if (status != 200) {
            throw new MtlsMsiException(
                    "mTLS token endpoint " + urlStr + " returned HTTP " + status + ": " + body);
        }
        return body;
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        return sb.toString();
    }

    // ─── Response parsers ────────────────────────────────────────────────────

    private static MtlsMsiHelperResult parseTokenResponse(String json, MtlsBindingInfo binding)
            throws MtlsMsiException {
        if (json == null || json.isEmpty()) {
            throw new MtlsMsiException("mTLS token endpoint returned empty response");
        }

        String accessToken = extractJsonString(json, "access_token");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new MtlsMsiException("mTLS token response missing access_token: " + json);
        }

        String tokenType = extractJsonString(json, "token_type");
        int expiresIn    = extractJsonInt(json, "expires_in");

        // Encode the binding certificate as PEM for callers who need it.
        String bindingCertPem = null;
        try {
            byte[] derBytes = binding.certificate.getEncoded();
            String b64      = Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(derBytes);
            bindingCertPem  = "-----BEGIN CERTIFICATE-----\n" + b64 + "\n-----END CERTIFICATE-----\n";
        } catch (CertificateEncodingException ignored) {}

        return new MtlsMsiHelperResult(accessToken, tokenType != null ? tokenType : "mtls_pop",
                expiresIn, bindingCertPem, binding.tenantId, binding.clientId);
    }

    // ─── Minimal JSON extractors ──────────────────────────────────────────────

    static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length()) return null;
        if (json.charAt(valueStart) == '"') {
            int end = valueStart + 1;
            while (end < json.length()) {
                char c = json.charAt(end);
                if (c == '\\') { end += 2; continue; }
                if (c == '"')  break;
                end++;
            }
            return json.substring(valueStart + 1, end)
                    .replace("\\n",  "\n")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\");
        }
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
        int valueEnd = valueStart;
        while (valueEnd < json.length()
                && (Character.isDigit(json.charAt(valueEnd)) || json.charAt(valueEnd) == '-')) {
            valueEnd++;
        }
        try {
            return Integer.parseInt(json.substring(valueStart, valueEnd));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
