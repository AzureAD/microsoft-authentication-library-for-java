// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URL;

public class GenericAuthority extends Authority {
    static final String AUTHORIZATION_ENDPOINT = "oauth2/v2.0/authorize";
    static final String TOKEN_ENDPOINT = "oauth2/v2.0/token";
    static final String DEVICE_CODE_ENDPOINT = "oauth2/v2.0/devicecode";

    //Part of the OpenIdConnect standard, this is appended to the authority to create the endpoint that has OIDC metadata
    static final String WELL_KNOWN_OPENID_CONFIGURATION = ".well-known/openid-configuration";

    private static final String AUTHORITY_FORMAT = "https://%s/%s/";
    private static final String DEVICE_CODE_ENDPOINT_FORMAT = AUTHORITY_FORMAT + DEVICE_CODE_ENDPOINT;
    private static final String AUTHORIZATION_ENDPOINT_FORMAT = AUTHORITY_FORMAT + AUTHORIZATION_ENDPOINT;
    private static final String TOKEN_ENDPOINT_FORMAT = AUTHORITY_FORMAT + TOKEN_ENDPOINT;

    GenericAuthority(URL authorityUrl) throws MalformedURLException {
        super(transformAuthority(authorityUrl), AuthorityType.GENERIC);

        setAuthorityProperties();
        this.authority = String.format(AUTHORITY_FORMAT, host, tenant);
    }

    private static URL transformAuthority(URL originalAuthority) throws MalformedURLException {
        String transformedAuthority = originalAuthority.toString();
        transformedAuthority += WELL_KNOWN_OPENID_CONFIGURATION;

        return new URL(transformedAuthority);
    }

    private void setAuthorityProperties() {
        this.authorizationEndpoint = String.format(AUTHORIZATION_ENDPOINT_FORMAT, host, tenant);
        this.tokenEndpoint = String.format(TOKEN_ENDPOINT_FORMAT, host, tenant);
        this.deviceCodeEndpoint = String.format(DEVICE_CODE_ENDPOINT_FORMAT, host, tenant);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
