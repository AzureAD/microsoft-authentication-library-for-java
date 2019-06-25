package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Represents Authentication Authority responsible for issuing access tokens.
 */

@Accessors(fluent=true)
@Getter(AccessLevel.PACKAGE)
abstract class Authority {

    private static final String ADFS_PATH_SEGMENT = "adfs";
    private static final String B2C_PATH_SEGMENT = "tfp";

    final static String TOKEN_ENDPOINT = "/oauth2/v2.0/token";
    private final static String USER_REALM_ENDPOINT = "common/userrealm";
    private final static String userRealmEndpointFormat = "https://%s/" + USER_REALM_ENDPOINT + "/%s?api-version=1.0";

    String authority;
    final URL canonicalAuthorityUrl;
    protected final AuthorityType authorityType;
    String selfSignedJwtAudience;

    String host;
    String tenant;
    boolean isTenantless;

    String tokenEndpoint;

    URL tokenEndpointUrl() throws MalformedURLException {
        return new URL(tokenEndpoint);
    }

    Authority(URL canonicalAuthorityUrl){
        this.canonicalAuthorityUrl = canonicalAuthorityUrl;
        this.authorityType = detectAuthorityType(canonicalAuthorityUrl);
        setCommonAuthorityProperties();
    }

    private void setCommonAuthorityProperties() {
        this.tenant = getTenant(canonicalAuthorityUrl, authorityType);
        this.host = canonicalAuthorityUrl.getAuthority().toLowerCase();
    }

    static Authority createAuthority(URL authorityUrl){
       AuthorityType authorityType = detectAuthorityType(authorityUrl);
       if(authorityType == AuthorityType.AAD){
           return new AADAuthority(authorityUrl);
       } else if(authorityType == AuthorityType.B2C) {
           return new B2CAuthority(authorityUrl);
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
                    AuthenticationErrorMessage.AUTHORITY_URI_INVALID_PATH);
        }

        final String firstPath = path.substring(0, path.indexOf("/"));


        if(isB2CAuthority(firstPath)){
            return AuthorityType.B2C;
        } else if(isAdfsAuthority(firstPath)) {
            return AuthorityType.ADFS;
        } else {
            return AuthorityType.AAD;
        }
    }

    void validateAuthorityUrl() {
        if (!this.canonicalAuthorityUrl.getProtocol().equalsIgnoreCase("https")) {
            throw new IllegalArgumentException(
                    AuthenticationErrorMessage.AUTHORITY_URI_INSECURE);
        }

        if (this.canonicalAuthorityUrl.toString().contains("#")) {
            throw new IllegalArgumentException(
                    "authority is invalid format (contains fragment)");
        }

        if (!StringHelper.isBlank(this.canonicalAuthorityUrl.getQuery())) {
            throw new IllegalArgumentException(
                    "authority cannot contain query parameters");
        }
    }

    static String getTenant(URL authorityUrl, AuthorityType authorityType) {
        String[] segments = authorityUrl.getPath().substring(1).split("/");
        if(authorityType == AuthorityType.B2C){
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

    private static boolean isB2CAuthority(final String firstPath){
        return firstPath.compareToIgnoreCase(B2C_PATH_SEGMENT) == 0;
    }
}
