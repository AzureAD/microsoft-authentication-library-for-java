// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.URL;

@Accessors(fluent=true)
@Getter(AccessLevel.PACKAGE)
class B2CAuthority extends Authority{

    private final static String AUTHORIZATION_ENDPOINT = "/oauth2/v2.0/authorize";
    private final static String TOKEN_ENDPOINT = "/oauth2/v2.0/token";

    private final static String B2C_AUTHORIZATION_ENDPOINT_FORMAT = "https://%s/%s/%s" + AUTHORIZATION_ENDPOINT;
    private final static String B2C_TOKEN_ENDPOINT_FORMAT = "https://%s/%s" + TOKEN_ENDPOINT + "?p=%s";
    private String policy;

    B2CAuthority(final URL authorityUrl){
        super(authorityUrl);
        validateAuthorityUrl();
        setAuthorityProperties();
    }

    private void setAuthorityProperties() {
        String[] segments = canonicalAuthorityUrl.getPath().substring(1).split("/");

        if(segments.length < 3){
            throw new IllegalArgumentException(
                    "B2C 'authority' Uri should have at least 3 segments in the path " +
                            "(i.e. https://<host>/tfp/<tenant>/<policy>/...)");
        }
        policy = segments[2];

        final String b2cAuthorityFormat = "https://%s/%s/%s/%s/";
        this.authority = String.format(
                b2cAuthorityFormat,
                canonicalAuthorityUrl.getAuthority(),
                segments[0],
                segments[1],
                segments[2]);

        this.authorizationEndpoint = String.format(B2C_TOKEN_ENDPOINT_FORMAT, host, tenant, policy);
        this.tokenEndpoint = String.format(B2C_TOKEN_ENDPOINT_FORMAT, host, tenant, policy);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
