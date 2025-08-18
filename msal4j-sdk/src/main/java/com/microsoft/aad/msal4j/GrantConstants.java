// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Constants used by {@link OAuthAuthorizationGrant} and related classes
 */
class GrantConstants {

    //Parameter names
    static final String GRANT_TYPE_PARAMETER = "grant_type";
    static final String ASSERTION_PARAMETER = "assertion";
    static final String USERNAME_PARAMETER = "username";
    static final String PASSWORD_PARAMETER = "password";

    //Grant types
    static final String AUTHORIZATION_CODE = "authorization_code";
    static final String CLIENT_CREDENTIALS = "client_credentials";
    static final String PASSWORD = "password";
    static final String SAML_2_BEARER = "urn:ietf:params:oauth:grant-type:saml2-bearer";
    static final String SAML_1_1_BEARER = "urn:ietf:params:oauth:grant-type:saml1_1-bearer";
    static final String JWT_BEARER = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    static final String REFRESH_TOKEN = "refresh_token";
    static final String DEVICE_CODE = "device_code";
}
