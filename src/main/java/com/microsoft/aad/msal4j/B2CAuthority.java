// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.URL;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class B2CAuthority extends Authority {

    private final static String AUTHORIZATION_ENDPOINT = "/oauth2/v2.0/authorize";
    private final static String TOKEN_ENDPOINT = "/oauth2/v2.0/token";

    private final static String B2C_AUTHORIZATION_ENDPOINT_FORMAT = "https://%s/%s/%s" + AUTHORIZATION_ENDPOINT;
    private final static String B2C_TOKEN_ENDPOINT_FORMAT = "https://%s/%s" + TOKEN_ENDPOINT + "?p=%s";
    private String policy;

    B2CAuthority(final URL authorityUrl) {
        super(authorityUrl, AuthorityType.B2C);
        setAuthorityProperties();
    }

    private void validatePathSegments(String[] segments) {
        if (segments.length < 2) {
            throw new IllegalArgumentException(
                    "Valid B2C 'authority' URLs should follow either of these formats: https://<host>/<tenant>/<policy>/... or https://<host>/something/<tenant>/<policy>/...");
        }
    }

    private void setAuthorityProperties() {
        String[] segments = canonicalAuthorityUrl.getPath().substring(1).split("/");

        // In the early days of MSAL, the only way for the library to identify a B2C authority was whether or not the authority
        //   had three segments in the path, and the first segment was 'tfp'. Valid B2C authorities looked like: https://<host>/tfp/<tenant>/<policy>/...
        //
        // More recent changes to B2C should ensure that any new B2C authorities have 'b2clogin.com' in the host of the URL,
        //   so app developers shouldn't need to add 'tfp' and the first path segment should just be the tenant: https://<something>.b2clogin.com/<tenant>/<policy>/...
        //
        // However, legacy URLs using the old format must still be supported by these sorts of checks here and elsewhere, so for the near
        //   future at least we must consider both formats as valid until we're either sure all customers are swapped,
        //   or until we're comfortable with a potentially breaking change
        validatePathSegments(segments);

        try {
            policy = segments[2];
            this.authority = String.format(
                    "https://%s/%s/%s/%s/",
                    canonicalAuthorityUrl.getAuthority(),
                    segments[0],
                    segments[1],
                    segments[2]);
        } catch (IndexOutOfBoundsException e){
            policy = segments[1];
            this.authority = String.format(
                    "https://%s/%s/%s/",
                    canonicalAuthorityUrl.getAuthority(),
                    segments[0],
                    segments[1]);
        }

        this.authorizationEndpoint = String.format(B2C_AUTHORIZATION_ENDPOINT_FORMAT, host, tenant, policy);
        this.tokenEndpoint = String.format(B2C_TOKEN_ENDPOINT_FORMAT, host, tenant, policy);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}
