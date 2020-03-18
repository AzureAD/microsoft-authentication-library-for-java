// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;

class ADFSAuthority extends Authority{

    final static String AUTHORIZATION_ENDPOINT = "oauth2/authorize";
    final static String TOKEN_ENDPOINT = "oauth2/token";

    private final static String ADFS_AUTHORITY_FORMAT = "https://%s/%s/";

    ADFSAuthority(final URL authorityUrl) {
        super(authorityUrl);
        this.authority = String.format(ADFS_AUTHORITY_FORMAT, host, tenant);
        this.authorizationEndpoint = authority + AUTHORIZATION_ENDPOINT;
        this.tokenEndpoint = authority + TOKEN_ENDPOINT;
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
