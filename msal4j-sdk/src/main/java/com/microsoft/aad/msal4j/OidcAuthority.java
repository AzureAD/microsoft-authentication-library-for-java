// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class OidcAuthority extends Authority {
    //Part of the OpenIdConnect standard, this is appended to the authority to create the endpoint that has OIDC metadata
    private static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";
    private static final String AUTHORITY_FORMAT = "https://%s/%s/";
    private static final String CIAM_AUTHORITY_FORMAT = "https://%s.ciamlogin.com/%s";

    OidcAuthority(URL authorityUrl) throws MalformedURLException {
        super(createOidcDiscoveryUrl(authorityUrl), AuthorityType.OIDC);

        this.authority = String.format(AUTHORITY_FORMAT, host, tenant);
    }

    private static URL createOidcDiscoveryUrl(URL originalAuthority) throws MalformedURLException {
        String authority = originalAuthority.toString();
        authority += WELL_KNOWN_OPENID_CONFIGURATION;

        return new URL(authority);
    }

    void setAuthorityProperties(OidcDiscoveryResponse instanceDiscoveryResponse) {
        validateIssuer(instanceDiscoveryResponse.issuer());

        this.authorizationEndpoint = instanceDiscoveryResponse.authorizationEndpoint();
        this.tokenEndpoint = instanceDiscoveryResponse.tokenEndpoint();
        this.deviceCodeEndpoint = instanceDiscoveryResponse.deviceCodeEndpoint();
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }

    private void validateIssuer(String issuerFromOidcDiscovery) {
        if (!isIssuerValid(issuerFromOidcDiscovery)) {
            throw new MsalClientException(
                    String.format("Invalid issuer from OIDC discovery. Issuer %s does not match authority %s, or is in an unexpected format", issuerFromOidcDiscovery, canonicalAuthorityUrl),
                    "issuer_validation");
        }
    }

    /**
     * Validates the issuer from OIDC discovery.
     * Issuer is valid if it matches the authority URL (without the well-known segment)
     * or uses a known Microsoft host. Regional Microsoft issuers require a known
     * Microsoft authority; other authorities can also use the existing CIAM mapping.
     *
     * @return true if the issuer is valid, false otherwise
     */
    private boolean isIssuerValid(String issuerFromOidcDiscovery) {
        if (issuerFromOidcDiscovery == null) {
            return false;
        }

        // Case 1: Check against canonicalAuthorityUrl without the well-known segment
        String authorityWithoutWellKnown = canonicalAuthorityUrl.toString();
        if (authorityWithoutWellKnown.endsWith(WELL_KNOWN_OPENID_CONFIGURATION)) {
            authorityWithoutWellKnown = authorityWithoutWellKnown.substring(0,
                    authorityWithoutWellKnown.length() - WELL_KNOWN_OPENID_CONFIGURATION.length());

            // Normalize both URLs to ensure consistent comparison
            String normalizedAuthority = Authority.enforceTrailingSlash(authorityWithoutWellKnown);
            String normalizedIssuer = Authority.enforceTrailingSlash(issuerFromOidcDiscovery);

            if (normalizedIssuer.equals(normalizedAuthority)) {
                return true;
            }
        }

        if (isMicrosoftIssuer(issuerFromOidcDiscovery)) {
            return true;
        }

        // A known Microsoft authority must not reach the cross-host CIAM fallback.
        if (isKnownMicrosoftAuthority()) {
            return false;
        }

        // Check CIAM format: "https://{tenant}.ciamlogin.com/{tenant}"
        if (!StringHelper.isNullOrBlank(tenant)) {
            String ciamPattern = String.format(CIAM_AUTHORITY_FORMAT, tenant, tenant);
            return issuerFromOidcDiscovery.startsWith(ciamPattern);
        }

        return false;
    }

    private boolean isMicrosoftIssuer(String issuerFromOidcDiscovery) {
        URI issuer;
        try {
            issuer = new URI(issuerFromOidcDiscovery);
        } catch (URISyntaxException e) {
            return false;
        }
        if (!"https".equalsIgnoreCase(issuer.getScheme()) || issuer.getHost() == null
                || issuer.getRawUserInfo() != null || issuer.getRawQuery() != null
                || issuer.getRawFragment() != null || issuer.getPort() > 65535) {
            return false;
        }

        String issuerHost = issuer.getHost();
        if (AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(issuerHost)) {
            return true;
        }

        int firstDot = issuerHost.indexOf('.');
        return isKnownMicrosoftAuthority()
                && firstDot > 0
                && AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(issuerHost.substring(firstDot + 1));
    }

    private boolean isKnownMicrosoftAuthority() {
        String authorityHost = canonicalAuthorityUrl.getHost();
        // A final DNS root dot changes neither host identity nor eligibility; preserve the discovery URL.
        if (authorityHost.endsWith(".")) {
            authorityHost = authorityHost.substring(0, authorityHost.length() - 1);
        }
        return AadInstanceDiscoveryProvider.TRUSTED_HOSTS_SET.contains(authorityHost);
    }
}
