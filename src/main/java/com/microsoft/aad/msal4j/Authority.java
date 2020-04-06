// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents Authentication Authority responsible for issuing access tokens.
 */

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
abstract class Authority {

    private static final String ADFS_PATH_SEGMENT = "adfs";
    private static final String B2C_PATH_SEGMENT = "tfp";

    private final static String USER_REALM_ENDPOINT = "common/userrealm";
    private final static String userRealmEndpointFormat = "https://%s/" + USER_REALM_ENDPOINT + "/%s?api-version=1.0";

    String authority;
    final URL canonicalAuthorityUrl;
    protected final AuthorityType authorityType;
    String selfSignedJwtAudience;

    String host;
    String tenant;
    boolean isTenantless;

    String authorizationEndpoint;
    String tokenEndpoint;

    URL tokenEndpointUrl() throws MalformedURLException {
        return new URL(tokenEndpoint);
    }

    Authority(URL canonicalAuthorityUrl, AuthorityType authorityType) {
        this.canonicalAuthorityUrl = canonicalAuthorityUrl;
        this.authorityType = authorityType;
        setCommonAuthorityProperties();
    }

    private void setCommonAuthorityProperties() {
        this.tenant = getTenant(canonicalAuthorityUrl, authorityType);
        this.host = canonicalAuthorityUrl.getAuthority().toLowerCase();
    }

    static Authority createAuthority(URL authorityUrl) {
        validateAuthority(authorityUrl);

        AuthorityType authorityType = detectAuthorityType(authorityUrl);
        if (authorityType == AuthorityType.AAD) {
            return new AADAuthority(authorityUrl);
        } else if (authorityType == AuthorityType.B2C) {
            return new B2CAuthority(authorityUrl);
        } else if (authorityType == AuthorityType.ADFS) {
            return new ADFSAuthority(authorityUrl);
        } else {
            throw new IllegalArgumentException("Unsupported Authority Type");
        }
    }

    static AuthorityType detectAuthorityType(URL authorityUrl) {
        if (authorityUrl == null) {
            throw new NullPointerException("canonicalAuthorityUrl");
        }

        final String path = authorityUrl.getPath().substring(1);
        if (StringHelper.isBlank(path)) {
            throw new IllegalArgumentException(
                    "authority Uri should have at least one segment in the path (i.e. https://<host>/<path>/...)");
        }

        final String firstPath = path.substring(0, path.indexOf("/"));

        if (isB2CAuthority(firstPath)) {
            return AuthorityType.B2C;
        } else if (isAdfsAuthority(firstPath)) {
            return AuthorityType.ADFS;
        } else {
            return AuthorityType.AAD;
        }
    }

    static void validateAuthority(URL authorityUrl) {
        if (!authorityUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException(
                    "authority should use the 'https' scheme");
        }

        if (authorityUrl.toString().contains("#")) {
            throw new IllegalArgumentException(
                    "authority is invalid format (contains fragment)");
        }

        if (!StringHelper.isBlank(authorityUrl.getQuery())) {
            throw new IllegalArgumentException(
                    "authority cannot contain query parameters");
        }

        final String path = authorityUrl.getPath();

        if (path.length() == 0) {
            throw new IllegalArgumentException(
                    IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH);
        }

        String[] segments = path.substring(1).split("/");

        if (segments.length == 0) {
            throw new IllegalArgumentException(
                    IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH_SEGMENT);
        }

        for (String segment : segments) {
            if (StringHelper.isBlank(segment)) {
                throw new IllegalArgumentException(
                        IllegalArgumentExceptionMessages.AUTHORITY_URI_EMPTY_PATH_SEGMENT);
            }
        }
    }

    static String getTenant(URL authorityUrl, AuthorityType authorityType) {
        String[] segments = authorityUrl.getPath().substring(1).split("/");
        if (authorityType == AuthorityType.B2C) {
            return segments[1];
        }
        return segments[0];
    }

    String getUserRealmEndpoint(String username) {
        return String.format(userRealmEndpointFormat, host, username);
    }

    private static boolean isAdfsAuthority(final String firstPath) {
        return firstPath.compareToIgnoreCase(ADFS_PATH_SEGMENT) == 0;
    }

    private static boolean isB2CAuthority(final String firstPath) {
        return firstPath.compareToIgnoreCase(B2C_PATH_SEGMENT) == 0;
    }
}
