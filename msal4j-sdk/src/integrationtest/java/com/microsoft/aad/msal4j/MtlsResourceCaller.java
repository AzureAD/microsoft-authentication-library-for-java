// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    private MtlsResourceCaller() {
    }

    /**
     * GETs {@code resourceUrl}, presenting {@code bindingCert} as the client TLS certificate and the
     * {@code mtls_pop} access token, and returns the HTTP status code.
     *
     * <p>The response body is drained and discarded — it must never be logged in (public) CI. Only the
     * status code is needed for the caller's assertion.
     *
     * @param resourceUrl the mTLS-enabled resource endpoint (e.g. {@code https://mtlstb.graph.microsoft.com/...})
     * @param accessToken the mTLS-bound PoP access token
     * @param bindingCert the certificate the token is bound to; must expose its private key for the handshake
     * @return the HTTP status code returned by the resource
     */
    static int callResourceWithMtlsToken(String resourceUrl, String accessToken, IClientCertificate bindingCert)
            throws IOException {
        SSLSocketFactory socketFactory = MtlsClientCertificateHelper.createMtlsSocketFactory(bindingCert);

        HttpsURLConnection connection = (HttpsURLConnection) new URL(resourceUrl).openConnection();
        try {
            connection.setSSLSocketFactory(socketFactory);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Authorization", "mtls_pop " + accessToken);
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(30_000);

            int status = connection.getResponseCode();
            drainAndClose(status < HttpURLConnection.HTTP_BAD_REQUEST
                    ? connection.getInputStream() : connection.getErrorStream());
            return status;
        } finally {
            connection.disconnect();
        }
    }

    // Reads and discards the response body so the connection can be released cleanly. The external body
    // is intentionally never logged (it can be large and is not needed for the status-code assertion).
    private static void drainAndClose(InputStream stream) throws IOException {
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
}
