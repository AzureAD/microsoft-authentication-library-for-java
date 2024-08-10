// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class GenericAuthority extends Authority {
    //Part of the OpenIdConnect standard, this is appended to the authority to create the endpoint that has OIDC metadata
    static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";
    private static final String AUTHORITY_FORMAT = "https://%s/%s/";

    GenericAuthority(URL authorityUrl) throws MalformedURLException {
        super(transformAuthority(authorityUrl), AuthorityType.GENERIC);

        this.authority = String.format(AUTHORITY_FORMAT, host, tenant);
    }

    private static URL transformAuthority(URL originalAuthority) throws MalformedURLException {
        String transformedAuthority = originalAuthority.toString();
        transformedAuthority += WELL_KNOWN_OPENID_CONFIGURATION;

        return new URL(transformedAuthority);
    }

    void setAuthorityProperties(AadInstanceDiscoveryResponse instanceDiscoveryResponse) {
        this.authorizationEndpoint = instanceDiscoveryResponse.authorizationEndpoint();
        this.tokenEndpoint = instanceDiscoveryResponse.tokenEndpoint();
        this.deviceCodeEndpoint = instanceDiscoveryResponse.deviceCodeEndpoint();
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
