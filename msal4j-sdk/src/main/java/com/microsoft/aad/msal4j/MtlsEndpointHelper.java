// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

/**
 * Derives the mutual-TLS (mTLS) token endpoint used for mTLS Proof-of-Possession requests by rewriting the
 * standard token endpoint host ({@code login.*}) to the mTLS host ({@code mtlsauth.*}).
 *
 * <ul>
 *   <li>No region configured: {@code https://mtlsauth.microsoft.com/<tenant>/oauth2/v2.0/token} — the
 *   global mTLS host is production-ready (ESTS-R regional failover).</li>
 *   <li>Region configured: {@code https://<region>.mtlsauth.microsoft.com/<tenant>/oauth2/v2.0/token}.</li>
 * </ul>
 *
 * <p>Region is OPTIONAL — there is no "region required" error path. The authority must be tenanted
 * ({@code /common} and {@code /organizations} are rejected). US Gov / China (and other sovereign clouds)
 * are fail-fast for now; the guardrail is isolated in {@link #isMtlsPoPUnsupportedCloud(String)} so it is
 * trivial to lift per-cloud once {@code mtlsauth.*} lands there.
 */
final class MtlsEndpointHelper {

    static final String GLOBAL_MTLS_HOST = "mtlsauth.microsoft.com";

    private static final String REGIONAL_LOGIN_SUFFIX = ".login.microsoft.com";

    private MtlsEndpointHelper() {
    }

    /**
     * Derives the mTLS token endpoint URL from a standard (already host-regionalized) token endpoint URL.
     * The tenant is taken from the endpoint path and must be a real tenant (not {@code common}/{@code organizations}).
     *
     * @param tokenEndpoint the standard token endpoint URL (e.g. {@code https://login.microsoftonline.com/<tenant>/oauth2/v2.0/token})
     * @return the mTLS token endpoint URL
     */
    static URL deriveMtlsTokenEndpoint(URL tokenEndpoint) {
        validateTenanted(extractTenant(tokenEndpoint));

        String host = tokenEndpoint.getHost();
        if (isMtlsPoPUnsupportedCloud(host)) {
            throw new MsalClientException(
                    "mTLS Proof-of-Possession is not supported in this cloud (host: " + host + "). " +
                            "It is currently available in the public cloud only.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }

        String mtlsHost = deriveMtlsHost(host);

        try {
            return new URL(tokenEndpoint.getProtocol(), mtlsHost, tokenEndpoint.getPort(), tokenEndpoint.getFile());
        } catch (MalformedURLException e) {
            throw new MsalClientException(
                    "Failed to derive the mTLS token endpoint: " + e.getMessage(),
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }

    /**
     * Extracts the tenant (first path segment) from a token endpoint URL.
     */
    static String extractTenant(URL tokenEndpoint) {
        String path = tokenEndpoint.getPath();
        if (StringHelper.isBlank(path)) {
            return null;
        }
        String[] segments = path.split("/");
        for (String segment : segments) {
            if (!StringHelper.isBlank(segment)) {
                return segment;
            }
        }
        return null;
    }

    /**
     * Rewrites a {@code login.*} host to its {@code mtlsauth.*} equivalent, preserving any regional prefix.
     */
    static String deriveMtlsHost(String loginHost) {
        String lower = loginHost.toLowerCase(Locale.ROOT);

        // Regionalized ESTS-R host: <region>.login.microsoft.com -> <region>.mtlsauth.microsoft.com
        if (lower.endsWith(REGIONAL_LOGIN_SUFFIX) && lower.length() > REGIONAL_LOGIN_SUFFIX.length()) {
            String region = lower.substring(0, lower.length() - REGIONAL_LOGIN_SUFFIX.length());
            return region + "." + GLOBAL_MTLS_HOST;
        }

        // Global public hosts (login.microsoftonline.com, login.microsoft.com, login.windows.net, etc.)
        return GLOBAL_MTLS_HOST;
    }

    /**
     * Isolated sovereign-cloud guardrail. Returns {@code true} for clouds where mTLS PoP is not yet
     * supported (US Gov, China). Keep this as the single point of truth so it is trivial to lift per-cloud.
     */
    static boolean isMtlsPoPUnsupportedCloud(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        return lower.endsWith(".us")
                || lower.endsWith(".cn")
                || lower.contains("usgovcloudapi")
                || lower.contains("microsoftonline.us")
                || lower.contains("chinacloudapi.cn")
                || lower.contains("partner.microsoftonline.cn");
    }

    private static void validateTenanted(String tenant) {
        if (StringHelper.isBlank(tenant)
                || "common".equalsIgnoreCase(tenant)
                || "organizations".equalsIgnoreCase(tenant)) {
            throw new MsalClientException(
                    "mTLS Proof-of-Possession requires a tenanted authority. The '/common' and " +
                            "'/organizations' authorities are not supported; specify a tenant ID or domain.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }
    }
}
