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

final class MtlsEndpointHelper {
    private static final String GLOBAL_MTLS_HOST = "mtlsauth.microsoft.com";
    private static final String REGIONAL_LOGIN_SUFFIX = ".login.microsoft.com";
    private static final Set<String> UNSUPPORTED_HOSTS = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList("login.usgovcloudapi.net", "login.chinacloudapi.cn")));

    private MtlsEndpointHelper() {
    }

    static URL deriveMtlsTokenEndpoint(URL tokenEndpoint) {
        if (!"https".equalsIgnoreCase(tokenEndpoint.getProtocol())) {
            throw error("Certificate-based mTLS requires an HTTPS token endpoint.");
        }
        String tenant = firstPathSegment(tokenEndpoint);
        if (tenant.indexOf('%') >= 0) {
            throw error("Certificate-based mTLS does not accept percent-encoded tenant path segments.");
        }
        if (StringHelper.isBlank(tenant)
                || "common".equalsIgnoreCase(tenant)
                || "organizations".equalsIgnoreCase(tenant)
                || "consumers".equalsIgnoreCase(tenant)) {
            throw error("Certificate-based mTLS requires a concrete tenanted AAD authority.");
        }

        String host = tokenEndpoint.getHost().toLowerCase(Locale.ROOT);
        if (UNSUPPORTED_HOSTS.contains(host)) {
            throw error("Certificate-based mTLS is not supported for authority host '" + host + "'.");
        }
        String mtlsHost = deriveMtlsHost(host);
        if (mtlsHost == null) {
            throw error("Cannot safely derive an mtlsauth endpoint from authority host '" + host + "'.");
        }
        try {
            return new URL("https", mtlsHost, tokenEndpoint.getPort(), tokenEndpoint.getFile());
        } catch (MalformedURLException e) {
            throw error("Failed to derive the mTLS token endpoint: " + e.getMessage());
        }
    }

    static String deriveMtlsHost(String host) {
        String lower = host.toLowerCase(Locale.ROOT);
        if (lower.endsWith(REGIONAL_LOGIN_SUFFIX) && lower.length() > REGIONAL_LOGIN_SUFFIX.length()) {
            String region = lower.substring(0, lower.length() - REGIONAL_LOGIN_SUFFIX.length());
            if (region.indexOf('.') < 0 && AadInstanceDiscoveryProvider.isValidRegion(region)) {
                return region + "." + GLOBAL_MTLS_HOST;
            }
            return null;
        }
        if (AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(lower)
                && !AadInstanceDiscoveryProvider.TRUSTED_SOVEREIGN_HOSTS_SET.contains(lower)) {
            return GLOBAL_MTLS_HOST;
        }
        if (AadInstanceDiscoveryProvider.TRUSTED_SOVEREIGN_HOSTS_SET.contains(lower)
                && lower.startsWith("login.")) {
            return "mtlsauth" + lower.substring("login".length());
        }
        return null;
    }

    private static String firstPathSegment(URL endpoint) {
        for (String segment : endpoint.getPath().split("/")) {
            if (!StringHelper.isBlank(segment)) {
                return segment;
            }
        }
        return null;
    }

    private static MsalClientException error(String message) {
        return new MsalClientException(message, AuthenticationErrorCode.MTLS_POP_ERROR);
    }
}
