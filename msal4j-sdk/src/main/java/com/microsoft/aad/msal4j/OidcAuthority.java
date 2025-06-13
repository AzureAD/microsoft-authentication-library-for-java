// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class OidcAuthority extends Authority {
    //Part of the OpenIdConnect standard, this is appended to the authority to create the endpoint that has OIDC metadata
    static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";
    private static final String AUTHORITY_FORMAT = "https://%s/%s/";
    String issuerFromOidcDiscovery;

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
        this.authorizationEndpoint = instanceDiscoveryResponse.authorizationEndpoint();
        this.tokenEndpoint = instanceDiscoveryResponse.tokenEndpoint();
        this.deviceCodeEndpoint = instanceDiscoveryResponse.deviceCodeEndpoint();
        this.selfSignedJwtAudience = this.tokenEndpoint;
        this.issuerFromOidcDiscovery = instanceDiscoveryResponse.issuer();
    }

    /**
     * Validates the issuer from OIDC discovery.
     * Issuer is valid if it matches the authority URL (without the well-known segment)
     * or if it follows the CIAM issuer format.
     *
     * @return true if the issuer is valid, false otherwise
     */
    boolean isIssuerValid() {
        if (issuerFromOidcDiscovery == null) {
            return false;
        }

        // Normalize issuer by removing trailing slashes
        String normalizedIssuer = issuerFromOidcDiscovery;
        while (normalizedIssuer.endsWith("/")) {
            normalizedIssuer = normalizedIssuer.substring(0, normalizedIssuer.length() - 1);
        }

        // Case 1: Check against canonicalAuthorityUrl without the well-known segment
        String authorityWithoutWellKnown = canonicalAuthorityUrl.toString();
        if (authorityWithoutWellKnown.endsWith(WELL_KNOWN_OPENID_CONFIGURATION)) {
            authorityWithoutWellKnown = authorityWithoutWellKnown.substring(0,
                    authorityWithoutWellKnown.length() - WELL_KNOWN_OPENID_CONFIGURATION.length());

            // Remove trailing slash if present
            if (authorityWithoutWellKnown.endsWith("/")) {
                authorityWithoutWellKnown = authorityWithoutWellKnown.substring(0, authorityWithoutWellKnown.length() - 1);
            }

            if (normalizedIssuer.equals(authorityWithoutWellKnown)) {
                return true;
            }
        }

        // Case 2: Check CIAM format: "https://{tenant}.ciamlogin.com/{tenant}/"
        if (tenant != null && !tenant.isEmpty()) {
            String ciamPattern = "https://" + tenant + ".ciamlogin.com/" + tenant;
            return normalizedIssuer.startsWith(ciamPattern);
        }

        return false;
    }
}
