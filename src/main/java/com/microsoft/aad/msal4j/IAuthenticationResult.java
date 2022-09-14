// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;

/**
 * Interface representing the results of token acquisition operation.
 */
public interface IAuthenticationResult extends Serializable {

    /**
     * @return access token
     */
    String accessToken();

    /**
     * @return id token
     */
    String idToken();

    /**
     * @return user account
     */
    IAccount account();

    /**
     * @return tenant profile
     */
    ITenantProfile tenantProfile();

    /**
     * @return environment
     */
    String environment();

    /**
     * @return granted scopes values returned by the service
     */
    String scopes();

    /**
     * @return access token expiration date
     */
    java.util.Date expiresOnDate();

    /**
     * The source of the tokens in a result. Will simply be the word "cache" if retrieved from the in-memory token cache,
     * otherwise it will be the actual authority used by the library for the token request
     *
     * In general an authority value here will match the authority configured in the client app or acquire token request,
     * however there are certain cases, such as when using regional endpoints, where the library may use a different authority
     *
     * @return source of tokens in result
     */
    String tokenSource();
}
