// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
 * ({@code /common} and {@code /organizations} are rejected).
 *
 * <p>Cloud boundaries are enforced so a request is never rewritten to (and its client certificate never
 * presented at) a host in a different cloud:
 * <ul>
 *   <li>Two legacy sovereign hosts that have no {@code mtlsauth.*} endpoint — {@code login.usgovcloudapi.net}
 *   and {@code login.chinacloudapi.cn} — are rejected fast by {@link #isMtlsPoPUnsupportedCloud(String)}
 *   (mirrors MSAL.NET's {@code s_unsupportedMtlsHosts} denylist).</li>
 *   <li>Every other {@code login.*} host — including Azure Government and the current national clouds — is
 *   allowed: public global hosts collapse to {@code mtlsauth.microsoft.com}; any other {@code login.*} host
 *   is rewritten domain-preserving ({@code login} &rarr; {@code mtlsauth}) so it stays within its own cloud
 *   boundary rather than being sent to the public endpoint (mirrors MSAL.NET).</li>
 *   <li>A host that is not a recognizable {@code login.*} host is rejected.</li>
 * </ul>
 */
final class MtlsEndpointHelper {

    static final String GLOBAL_MTLS_HOST = "mtlsauth.microsoft.com";

    private static final String REGIONAL_LOGIN_SUFFIX = ".login.microsoft.com";

    private static final String LOGIN_LABEL = "login";
    private static final String MTLS_LABEL = "mtlsauth";
    private static final String LOGIN_LABEL_PREFIX = LOGIN_LABEL + ".";

    // mTLS PoP is unsupported ONLY for these two legacy sovereign hosts (they have no mtlsauth.* endpoint).
    // Every other login.* host — including Azure Government and the current national clouds — is allowed and
    // rewritten domain-preserving. Mirrors MSAL.NET's s_unsupportedMtlsHosts denylist. Hosts are lowercase;
    // callers lowercase the input before lookup.
    private static final Set<String> MTLS_UNSUPPORTED_HOSTS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("login.usgovcloudapi.net", "login.chinacloudapi.cn")));

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
                    "mTLS Proof-of-Possession is not supported for the legacy sovereign host '" + host +
                            "'. This host has no mtlsauth.* endpoint.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }

        String mtlsHost = deriveMtlsHost(host);
        if (mtlsHost == null) {
            throw new MsalClientException(
                    "mTLS Proof-of-Possession requires a Microsoft Entra 'login.*' authority host, but the " +
                            "token endpoint host '" + host + "' is not recognized. Configure a public-cloud " +
                            "Microsoft Entra authority.",
                    AuthenticationErrorCode.MTLS_POP_ERROR);
        }

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
     * Rewrites a {@code login.*} host to its {@code mtlsauth.*} equivalent, preserving both any regional
     * prefix and the cloud domain.
     *
     * <ul>
     *   <li>Regionalized public host {@code <region>.login.microsoft.com} &rarr; {@code <region>.mtlsauth.microsoft.com}.</li>
     *   <li>Public global hosts (e.g. {@code login.microsoftonline.com}, {@code login.windows.net},
     *   {@code login.microsoft.com}, {@code sts.windows.net}) &rarr; {@code mtlsauth.microsoft.com}.</li>
     *   <li>Any other {@code login.*} host &rarr; domain-preserving swap ({@code login} &rarr; {@code mtlsauth}),
     *   keeping the request within the same cloud boundary (mirrors MSAL.NET).</li>
     * </ul>
     *
     * @return the {@code mtlsauth.*} host, or {@code null} if {@code loginHost} is not a recognizable
     * {@code login.*} host and therefore cannot be safely rewritten
     */
    static String deriveMtlsHost(String loginHost) {
        String lower = loginHost.toLowerCase(Locale.ROOT);

        // Regionalized ESTS-R host: <region>.login.microsoft.com -> <region>.mtlsauth.microsoft.com
        if (lower.endsWith(REGIONAL_LOGIN_SUFFIX) && lower.length() > REGIONAL_LOGIN_SUFFIX.length()) {
            String region = lower.substring(0, lower.length() - REGIONAL_LOGIN_SUFFIX.length());
            return region + "." + GLOBAL_MTLS_HOST;
        }

        // Public global hosts all resolve to the single public mTLS host.
        if (isPublicHost(lower)) {
            return GLOBAL_MTLS_HOST;
        }

        // Any other login.* host: preserve the domain and only swap the leading "login" label for
        // "mtlsauth", so the request stays within the same cloud boundary (never rewritten to the public
        // endpoint). Mirrors MSAL.NET's domain-preserving rewrite.
        if (lower.startsWith(LOGIN_LABEL_PREFIX)) {
            return MTLS_LABEL + lower.substring(LOGIN_LABEL.length());
        }

        // Not a recognizable login.* host: cannot safely derive an mTLS host.
        return null;
    }

    /**
     * @return {@code true} if {@code host} is a known public (non-sovereign) Microsoft Entra host that
     * should resolve to the global public mTLS host {@code mtlsauth.microsoft.com}.
     */
    private static boolean isPublicHost(String host) {
        return AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(host)
                && !AadInstanceDiscoveryProvider.TRUSTED_SOVEREIGN_HOSTS_SET.contains(host);
    }

    /**
     * Isolated sovereign-cloud guardrail. Returns {@code true} only for the two legacy sovereign hosts that
     * have no {@code mtlsauth.*} endpoint ({@code login.usgovcloudapi.net}, {@code login.chinacloudapi.cn}).
     * Every other {@code login.*} host — including Azure Government and the current national clouds — is
     * supported and rewritten domain-preserving by {@link #deriveMtlsHost(String)}. Mirrors MSAL.NET's
     * {@code s_unsupportedMtlsHosts} denylist; keep this as the single point of truth.
     */
    static boolean isMtlsPoPUnsupportedCloud(String host) {
        return MTLS_UNSUPPORTED_HOSTS.contains(host.toLowerCase(Locale.ROOT));
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
