// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public class AuthenticationErrorCode {

    /**
     * In the context of device code user has not yet authenticated via browser
     */
    public final static String AUTHORIZATION_PENDING = "authorization_pending";

    /**
     * In the context of device code, this error happens when the device code has expired before
     * the user signed-in on another device (this is usually after 15 min)
     */
    public final static String CODE_EXPIRED = "code_expired";

    /**
     * Standard Oauth2 protocol error code. It indicates that the application needs to expose
     * the UI to the user so that user does an interactive action in order to get a new token
     */
    public final static String INVALID_GRANT = "invalid_grant";

    /**
     * WS-Trust Endpoint not found in Metadata document
     */
    public final static String WSTRUST_ENDPOINT_NOT_FOUND_IN_METADATA_DOCUMENT = "wstrust_endpoint_not_found";

    /**
     * Password is required for managed user. Will typically happen when trying to do integrated windows authentication
     * for managed users
     */
    public final static String PASSWORD_REQUIRED_FOR_MANAGED_USER = "password_required_for_managed_user";

    /**
     * User realm discovery failed
     */
    public final static String USER_REALM_DISCOVERY_FAILED = "user_realm_discovery_failed";

    /**
     * Unknown error occurred
     */
    public final static String UNKNOWN = "unknown";
}

