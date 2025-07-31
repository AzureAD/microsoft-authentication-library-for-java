// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Token result from external token provider
 */
public class TokenProviderResult {

    /**
     * Access token - mandatory
     */
    private String accessToken;
    
    /**
     * Tenant Id of the client application
     */
    private String tenantId;
    
    /**
     * Expiration of the token - mandatory
     */
    private long expiresInSeconds;
    
    /**
     * When the token should be refreshed proactively (optional)
     */
    private long refreshInSeconds;

    /**
     * Gets the access token.
     * 
     * @return the access token
     */
    public String getAccessToken() {
        return this.accessToken;
    }

    /**
     * Gets the tenant ID.
     * 
     * @return the tenant ID of the client application
     */
    public String getTenantId() {
        return this.tenantId;
    }

    /**
     * Gets the token expiration time in seconds.
     * 
     * @return number of seconds until the token expires
     */
    public long getExpiresInSeconds() {
        return this.expiresInSeconds;
    }

    /**
     * Gets the recommended time in seconds to proactively refresh the token.
     * 
     * @return number of seconds when the token should be refreshed proactively
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
     * @param tenantId the tenant ID of the client application to set
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    /**
     * Sets the token expiration time in seconds.
     * 
     * @param expiresInSeconds number of seconds until the token expires
     */
    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    /**
     * Sets the recommended time in seconds to proactively refresh the token.
     * 
     * @param refreshInSeconds number of seconds when the token should be refreshed proactively
     */
    public void setRefreshInSeconds(long refreshInSeconds) {
        this.refreshInSeconds = refreshInSeconds;
    }
}
