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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;

import lombok.AccessLevel;
import lombok.Getter;

/**
 * Represents Authentication Authority responsible for issuing access tokens.
 */
@Getter(AccessLevel.PACKAGE)
class AuthenticationAuthority {
    private final Logger log = LoggerFactory
            .getLogger(AuthenticationAuthority.class);

    private final static String TENANTLESS_TENANT_NAME = "common";
    private final static String DISCOVERY_ENDPOINT = "common/discovery/instance";
    private final static String TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private final static String USER_REALM_ENDPOINT = "common/userrealm";
    final static String DEVICE_CODE_ENDPOINT = "/oauth2/v2.0/devicecode";

    private final String userRealmEndpointFormat = "https://%s/" + USER_REALM_ENDPOINT + "/%s?api-version=1.0";

    private final String tokenEndpointFormat = "https://%s/{tenant}" + TOKEN_ENDPOINT;

    private final String devicecodeEndpointFormat = "https://%s/{tenant}" + DEVICE_CODE_ENDPOINT;

    private String authority = "https://%s/%s/";

    private String host;
    private String issuer;
    private String tokenEndpoint;
    private String deviceCodeEndpoint;

    protected final AuthorityType authorityType;
    private boolean isTenantless;
    private String tokenUri;
    private String selfSignedJwtAudience;

    private final URL authorityUrl;
    private String tenant;

    AuthenticationAuthority(final URL authorityUrl) {
        this.authorityUrl = authorityUrl;
        this.authorityType = detectAuthorityType(authorityUrl);
        validateAuthorityUrl();
        setupAuthorityProperties();
    }

    String getUserRealmEndpoint(String username) {
        return String.format(userRealmEndpointFormat, host, username);
    }

    void setSelfSignedJwtAudience(final String selfSignedJwtAudience) {
        this.selfSignedJwtAudience = selfSignedJwtAudience;
    }

    static String getTenant(URL authorityUrl) {
        String path = authorityUrl.getPath().substring(1);
        return path.substring(0, path.indexOf("/"));
    }

    void setupAuthorityProperties() {
        this.tenant = getTenant(authorityUrl);
        this.host = authorityUrl.getAuthority().toLowerCase();

        this.authority = String.format(this.authority, host, tenant);
        this.tokenEndpoint = String.format(this.tokenEndpointFormat, host);
        this.tokenEndpoint = this.tokenEndpoint.replace("{tenant}", tenant);
        this.tokenUri = this.tokenEndpoint;
        this.issuer = this.tokenUri;
        this.deviceCodeEndpoint = String.format(this.devicecodeEndpointFormat, host);
        this.deviceCodeEndpoint = this.deviceCodeEndpoint.replace("{tenant}", tenant);

        this.isTenantless = TENANTLESS_TENANT_NAME.equalsIgnoreCase(tenant);
        this.setSelfSignedJwtAudience(this.getIssuer());
    }

    static AuthorityType detectAuthorityType(URL authorityUrl) {
        if (authorityUrl == null) {
            throw new NullPointerException("authorityUrl");
        }

        final String path = authorityUrl.getPath().substring(1);
        if (StringHelper.isBlank(path)) {
            throw new IllegalArgumentException(
                    AuthenticationErrorMessage.AUTHORITY_URI_INVALID_PATH);
        }

        final String firstPath = path.substring(0, path.indexOf("/"));
        final AuthorityType authorityType = isAdfsAuthority(firstPath) ? AuthorityType.ADFS
                : AuthorityType.AAD;

        return authorityType;
    }

    void validateAuthorityUrl() {
        if (!this.authorityUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException(
                    AuthenticationErrorMessage.AUTHORITY_URI_INSECURE);
        }

        if (this.authorityUrl.toString().contains("#")) {
            throw new IllegalArgumentException(
                    "authority is invalid format (contains fragment)");
        }

        if (!StringHelper.isBlank(this.authorityUrl.getQuery())) {
            throw new IllegalArgumentException(
                    "authority cannot contain query parameters");
        }
    }

    static boolean isAdfsAuthority(final String firstPath) {
        return firstPath.compareToIgnoreCase("adfs") == 0;
    }
}
