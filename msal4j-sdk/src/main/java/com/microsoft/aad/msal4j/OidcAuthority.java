// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class OidcAuthority extends Authority {
    //Part of the OpenIdConnect standard, this is appended to the authority to create the endpoint that has OIDC metadata
    static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";
    private static final String AUTHORITY_FORMAT = "https://%s/%s/";

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
    }
}
