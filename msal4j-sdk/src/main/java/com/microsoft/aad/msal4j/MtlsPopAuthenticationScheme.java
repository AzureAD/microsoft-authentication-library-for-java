// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

/**
 * Constants and helpers for the mTLS Proof-of-Possession authentication scheme.
 *
 * <p>mTLS PoP tokens are acquired via a mutual-TLS connection to a special
 * {@code mtlsauth.*} endpoint. The token is cryptographically bound to the
 * client certificate used in the TLS handshake via the {@code cnf.x5t#S256} claim.</p>
 */
class MtlsPopAuthenticationScheme {

    /** Token type value returned in the response and used as the cache key discriminator. */
    static final String TOKEN_TYPE_MTLS_POP = "mtls_pop";

    /**
     * Computes the x5t#S256 thumbprint of a certificate: the Base64URL-encoded (no padding)
     * SHA-256 digest of the DER-encoded certificate bytes.
     *
     * <p>This value appears as the {@code cnf.x5t#S256} claim in the access token and is
     * stored as the {@code keyId} in the token cache entry to prevent cross-certificate
     * token reuse.</p>
     *
     * @param cert the X.509 certificate
     * @return Base64URL-encoded SHA-256 thumbprint without padding
     */
    static String computeX5tS256(X509Certificate cert) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] digest = sha256.digest(cert.getEncoded());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException | CertificateEncodingException e) {
            throw new MsalClientException(
                    "Failed to compute x5t#S256 thumbprint: " + e.getMessage(),
                    AuthenticationErrorCode.MSALRUNTIME_INTEROP_ERROR);
        }
    }

    /**
     * Builds the mTLS token endpoint URL for the given authority host and tenant.
     *
     * <p>Endpoint rules (from the msal-dotnet design spec):</p>
     * <ul>
     *   <li>Public cloud: {@code https://{region}.mtlsauth.microsoft.com/{tenantId}/oauth2/v2.0/token}</li>
     *   <li>Public cloud (no region): {@code https://mtlsauth.microsoft.com/{tenantId}/oauth2/v2.0/token}</li>
     *   <li>Sovereign clouds: replace the {@code login.} prefix in the host with {@code mtlsauth.}</li>
     *   <li>US Gov ({@code login.usgovcloudapi.net}) and China ({@code login.chinacloudapi.cn})
     *       are not supported and will throw {@link MsalClientException}.</li>
     * </ul>
     *
     * @param region       Azure region (e.g. {@code "eastus"}), or {@code null} to use the global endpoint
     * @param tenantId     the AAD tenant GUID or domain
     * @param authorityHost the authority hostname (e.g. {@code "login.microsoftonline.com"})
     * @return the full mTLS token endpoint URL
     * @throws MsalClientException if the authority is an unsupported sovereign cloud
     */
    static String buildMtlsTokenEndpoint(String region, String tenantId, String authorityHost) {
        if (authorityHost.contains("usgovcloudapi.net") || authorityHost.contains("chinacloudapi.cn")) {
            throw new MsalClientException(
                    "mTLS Proof-of-Possession is not supported for US Government or China cloud authorities. " +
                    "Authority: " + authorityHost,
                    AuthenticationErrorCode.INVALID_REQUEST);
        }

        String mtlsHost = toMtlsHost(authorityHost);
        String regional = (region != null && !region.isEmpty()) ? region + "." : "";
        return String.format("https://%s%s/%s/oauth2/v2.0/token", regional, mtlsHost, tenantId);
    }

    /**
     * Converts a standard authority host to its mTLS equivalent.
     *
     * <p>Mapping rules:</p>
     * <ul>
     *   <li>{@code login.microsoftonline.com} → {@code mtlsauth.microsoft.com} (public cloud)</li>
     *   <li>Any other {@code login.*} host → {@code mtlsauth.*} (sovereign clouds, replace prefix)</li>
     *   <li>Other hosts → returned as-is (DSTS and custom authorities)</li>
     * </ul>
     */
    private static String toMtlsHost(String host) {
        if ("login.microsoftonline.com".equals(host)) {
            return "mtlsauth.microsoft.com";
        }
        if (host.startsWith("login.")) {
            return "mtlsauth." + host.substring("login.".length());
        }
        return host;
    }
}
