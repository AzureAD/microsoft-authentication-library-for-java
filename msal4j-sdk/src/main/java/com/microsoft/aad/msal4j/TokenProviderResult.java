// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/// Token result from external token provider
public class TokenProviderResult {

    //Access token - mandatory
    private String accessToken;
    //tenant Id of the client application
    private String tenantId;
    //Expiration of the token - mandatory
    private long expiresInSeconds;
    //When the token be refreshed proactively (optional)
    private long refreshInSeconds;

    public String getAccessToken() {
        return this.accessToken;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public long getExpiresInSeconds() {
        return this.expiresInSeconds;
    }

    public long getRefreshInSeconds() {
        return this.refreshInSeconds;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public void setRefreshInSeconds(long refreshInSeconds) {
        this.refreshInSeconds = refreshInSeconds;
    }
}
