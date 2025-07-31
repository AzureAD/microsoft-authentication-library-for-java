// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents an OpenID Connect (OIDC) authority that handles authentication and token issuance.
 * This authority type supports the standard OIDC discovery process.
 */
public class OidcAuthority extends Authority {
    //Part of the OpenIdConnect standard, this is appended to the authority to create the endpoint that has OIDC metadata
    private static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";
    private static final String AUTHORITY_FORMAT = "https://%s/%s/";
    private static final String CIAM_AUTHORITY_FORMAT = "https://%s.ciamlogin.com/%s";

    private String issuerFromOidcDiscovery;

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

        validateIssuer();
    }

    private void validateIssuer() {
        if (!isIssuerValid()) {
            throw new MsalClientException(
                    String.format("Invalid issuer from OIDC discovery. Issuer %s does not match authority %s, or is in an unexpected format", issuerFromOidcDiscovery, canonicalAuthorityUrl),
                    "issuer_validation");
        }
    }

    /**
     * Validates the issuer from OIDC discovery.
     * Issuer is valid if it matches the authority URL (without the well-known segment)
     * or if it follows the CIAM issuer format.
     *
     * @return true if the issuer is valid, false otherwise
     */
    private boolean isIssuerValid() {
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

        // Case 2: Check CIAM format: "https://{tenant}.ciamlogin.com/{tenant}"
        if (!StringHelper.isNullOrBlank(tenant)) {
            String ciamPattern = String.format(CIAM_AUTHORITY_FORMAT, tenant, tenant);
            return issuerFromOidcDiscovery.startsWith(ciamPattern);
        }

        return false;
    }
}
