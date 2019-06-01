// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

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