// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Represents the result returned by an external token provider.
 * Contains the access token, tenant information, and expiration details.
 */
public class TokenProviderResult {

    //Access token - mandatory
    private String accessToken;
    //tenant Id of the client application
    private String tenantId;
    //Expiration of the token - mandatory
    private long expiresInSeconds;
    //When the token be refreshed proactively (optional)
    private long refreshInSeconds;

    /**
     * Gets the access token.
     *
     * @return the access token string
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    /**
     * Gets the tenant ID associated with the token.
     *
     * @return the tenant ID string
     */
    public String getTenantId() {
        return this.tenantId;
    }

    /**
     * Gets the token expiration time in seconds.
     *
     * @return the expiration time in seconds
     */
    public long getExpiresInSeconds() {
        return this.expiresInSeconds;
    }

    /**
     * Gets the token refresh time in seconds.
     *
     * @return the refresh time in seconds
     */
    public long getRefreshInSeconds() {
        return this.refreshInSeconds;
    }

    /**
     * Sets the access token.
     *
     * @param accessToken the access token to set
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Sets the tenant ID.
     *
     * @param tenantId the tenant ID to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Sets the token expiration time in seconds.
     *
     * @param expiresInSeconds the expiration time in seconds
     */
    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    /**
     * Sets the token refresh time in seconds.
     *
     * @param refreshInSeconds the refresh time in seconds
     */
    public void setRefreshInSeconds(long refreshInSeconds) {
        this.refreshInSeconds = refreshInSeconds;
    }
}
