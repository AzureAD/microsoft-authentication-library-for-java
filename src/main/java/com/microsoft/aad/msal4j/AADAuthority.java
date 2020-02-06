// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent=true)
@Getter(AccessLevel.PACKAGE)
class AADAuthority extends Authority {

    private final static String TENANTLESS_TENANT_NAME = "common";
    private final static String AUTHORIZATION_ENDPOINT = "oauth2/v2.0/authorize";
    private final static String TOKEN_ENDPOINT = "oauth2/v2.0/token";
    final static String DEVICE_CODE_ENDPOINT = "oauth2/v2.0/devicecode";

    private final static String AAD_AUTHORITY_FORMAT = "https://%s/%s/";
    private final static String AAD_AUTHORIZATION_ENDPOINT_FORMAT = AAD_AUTHORITY_FORMAT + AUTHORIZATION_ENDPOINT;
    private final static String AAD_TOKEN_ENDPOINT_FORMAT = AAD_AUTHORITY_FORMAT + TOKEN_ENDPOINT;
    private final static String DEVICE_CODE_ENDPOINT_FORMAT = AAD_AUTHORITY_FORMAT + DEVICE_CODE_ENDPOINT;

    String deviceCodeEndpoint;

    AADAuthority(final URL authorityUrl) {
        super(authorityUrl);
        validateAuthorityUrl();
        setAuthorityProperties();
        this.authority = String.format(AAD_AUTHORITY_FORMAT, host, tenant);
    }

    private void setAuthorityProperties() {
        this.authorizationEndpoint = String.format(AAD_AUTHORIZATION_ENDPOINT_FORMAT, host, tenant);
        this.tokenEndpoint = String.format(AAD_TOKEN_ENDPOINT_FORMAT, host, tenant);
        this.deviceCodeEndpoint = String.format(DEVICE_CODE_ENDPOINT_FORMAT, host, tenant);

        this.isTenantless = TENANTLESS_TENANT_NAME.equalsIgnoreCase(tenant);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}