// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Test-only helper that calls a protected resource with an mTLS Proof-of-Possession (PoP) token,
 * presenting the token's binding certificate as the client certificate on the TLS handshake.
 *
 * <p>Mirrors the resource-call contract validated by MSAL .NET (its {@code ResourceCaller}): the
 * binding certificate is the client TLS certificate, the token is sent as
 * {@code Authorization: mtls_pop <token>}, and a correctly bound token is accepted with HTTP 200. A
 * 401/403 means the certificate was not presented on the handshake or the {@code mtls_pop} scheme was
 * wrong. Reused by both the SNI and (later) FIC mTLS-PoP end-to-end tests, so acquiring a token is
 * proven to be genuinely usable rather than only well-formed.
 */
final class MtlsResourceCaller {

    // Cap on the diagnostic error-body snippet captured for a non-2xx response.
    private static final int MAX_ERROR_BODY_CHARS = 2048;

    private MtlsResourceCaller() {
    }

    /**
     * GETs {@code resourceUrl}, presenting {@code bindingCert} as the client TLS certificate and the
     * {@code mtls_pop} access token, and returns the HTTP status code plus (for a non-2xx response only)
     * a bounded snippet of the error body.
     *
     * <p>A successful (2xx) body is drained and discarded and never surfaced. For a non-2xx response a
     * bounded snippet of the error body is captured so a failing assertion is self-diagnosing: a Graph
     * {@code {"error":{"code":...,"message":...}}} distinguishes a 400 (malformed request / token shape /
     * audience — the request reached the resource) from a 401/403 (binding certificate not presented on
     * the handshake or wrong {@code mtls_pop} scheme).
     *
     * @param resourceUrl the mTLS-enabled resource endpoint (e.g. {@code https://mtlstb.graph.microsoft.com/...})
     * @param accessToken the mTLS-bound PoP access token
     * @param bindingCert the certificate the token is bound to; must expose its private key for the handshake
     * @return the resource {@link Response} (status code, and error-body snippet on non-2xx)
     */
    static Response callResourceWithMtlsToken(String resourceUrl, String accessToken, IClientCertificate bindingCert)
            throws IOException {
        SSLSocketFactory socketFactory = MtlsClientCertificateHelper.createMtlsSocketFactory(bindingCert);

        HttpsURLConnection connection = (HttpsURLConnection) new URL(resourceUrl).openConnection();
        try {
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "mtls_pop " + accessToken);
            // Set an explicit, valid Accept header. Without this the JDK's HttpURLConnection injects a
            // default of "text/html, image/gif, image/jpeg, *; q=.2, */*; q=.2" whose bare "*; q=.2" token
            // has no "/" and is rejected by Graph's strict MIME parser with HTTP 400 BadRequest (a false
            // negative unrelated to the mTLS binding). Other MSAL SDKs' HTTP clients don't send this
            // malformed default, so this is a Java-only, test-helper-only fix.
            connection.setRequestProperty("Accept", "application/json");
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(30_000);

            int status = connection.getResponseCode();
            if (status < HttpURLConnection.HTTP_BAD_REQUEST) {
                // Success: drain and discard the body — a successful resource body is never surfaced.
                drainAndDiscard(connection.getInputStream());
                return new Response(status, null);
            }
            // Non-2xx: capture a bounded snippet of the error body so a failing assertion pinpoints the
            // cause (e.g. Graph InvalidAuthenticationToken / InvalidRequest / audience) rather than a bare code.
            return new Response(status, readBounded(connection.getErrorStream()));
        } finally {
            connection.disconnect();
        }
    }

    /**
     * Outcome of an mTLS resource call: the HTTP status and, for a non-2xx response only, a bounded
     * snippet of the error body. A 2xx body is never captured, preserving the rule that a successful
     * resource body is not surfaced; the error snippet exists solely to make an assertion failure
     * self-diagnosing.
     */
    static final class Response {
        private final int statusCode;
        private final String errorBody;

        private Response(int statusCode, String errorBody) {
            this.statusCode = statusCode;
            this.errorBody = errorBody;
        }

        int statusCode() {
            return statusCode;
        }

        /** Bounded error-body snippet for a non-2xx response; empty string for a 2xx response. */
        String errorBody() {
            return errorBody == null ? "" : errorBody;
        }
    }

    // Reads and discards the response body so the connection can be released cleanly. A successful
    // resource body is intentionally never surfaced (it can be large and is not needed for the assertion).
    private static void drainAndDiscard(InputStream stream) throws IOException {
        if (stream == null) {
            return;
        }
        try (InputStream in = stream) {
            byte[] buffer = new byte[4096];
            while (in.read(buffer) != -1) {
                // discard
            }
        }
    }

    // Reads up to MAX_ERROR_BODY_CHARS bytes of a non-2xx error stream (UTF-8) for diagnostics only.
    private static String readBounded(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        try (InputStream in = stream) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) != -1 && out.size() <= MAX_ERROR_BODY_CHARS) {
                out.write(buffer, 0, n);
            }
            String body = new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
            return body.length() > MAX_ERROR_BODY_CHARS
                    ? body.substring(0, MAX_ERROR_BODY_CHARS) + "…(truncated)"
                    : body;
        }
    }
}
