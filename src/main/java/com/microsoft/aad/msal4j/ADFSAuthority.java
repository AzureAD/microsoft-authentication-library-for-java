// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;

class ADFSAuthority extends Authority{

    final static String AUTHORIZATION_ENDPOINT = "oauth2/authorize";
    final static String TOKEN_ENDPOINT = "oauth2/token";
    final static String DEVICE_CODE_ENDPOINT = "oauth2/devicecode";

    private final static String ADFS_AUTHORITY_FORMAT = "https://%s/%s/";
    private final static String DEVICE_CODE_ENDPOINT_FORMAT = ADFS_AUTHORITY_FORMAT + DEVICE_CODE_ENDPOINT;

    String deviceCodeEndpoint;

    ADFSAuthority(final URL authorityUrl) {
        super(authorityUrl, AuthorityType.ADFS);
        this.authority = String.format(ADFS_AUTHORITY_FORMAT, host, tenant);
        this.authorizationEndpoint = authority + AUTHORIZATION_ENDPOINT;
        this.tokenEndpoint = authority + TOKEN_ENDPOINT;
        this.selfSignedJwtAudience = this.tokenEndpoint;
        this.deviceCodeEndpoint = String.format(DEVICE_CODE_ENDPOINT_FORMAT, host, tenant);
    }

    String deviceCodeEndpoint() {
        return deviceCodeEndpoint;
    }
}
