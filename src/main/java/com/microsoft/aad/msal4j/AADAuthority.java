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

    private final static String AADAuthorityFormat = "https://%s/%s/";
    private final static String AADtokenEndpointFormat = "https://%s/{tenant}" + TOKEN_ENDPOINT;

    final static String DEVICE_CODE_ENDPOINT = "/oauth2/v2.0/devicecode";
    private final static String deviceCodeEndpointFormat = "https://%s/{tenant}" + DEVICE_CODE_ENDPOINT;

    String deviceCodeEndpoint;

    AADAuthority(final URL authorityUrl) {
        super(authorityUrl);
        validateAuthorityUrl();
        setAuthorityProperties();
        this.authority = String.format(AADAuthorityFormat, host, tenant);
    }

    private void setAuthorityProperties() {
        this.tokenEndpoint = String.format(AADtokenEndpointFormat, host);
        this.tokenEndpoint = this.tokenEndpoint.replace("{tenant}", tenant);

        this.deviceCodeEndpoint = String.format(this.deviceCodeEndpointFormat, host);
        this.deviceCodeEndpoint = this.deviceCodeEndpoint.replace("{tenant}", tenant);

        this.isTenantless = TENANTLESS_TENANT_NAME.equalsIgnoreCase(tenant);
        this.selfSignedJwtAudience = this.tokenEndpoint;
    }
}