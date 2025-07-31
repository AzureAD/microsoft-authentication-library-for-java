// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;

/**
 * The authentication parameters provided to the app token provider callback.
 */
public class AppTokenProviderParameters {

    /**
     * Specifies which scopes to request.
     */
    public Set<String> scopes;
    
    /**
     * Correlation id of the authentication request.
     */
    public String correlationId;
    
    /**
     * A string with one or multiple claims.
     */
    public String claims;
    
    /**
     * Tenant id for the authentication request.
     */
    public String tenantId;

    public AppTokenProviderParameters(Set<String> scopes, String correlationId, String claims, String tenantId) {
        this.scopes = scopes;
        this.correlationId = correlationId;
        this.claims = claims;
        this.tenantId = tenantId;
    }

    /**
     * Gets the scopes for this authentication request.
     * 
     * @return set of scopes being requested
     */
    public Set<String> getScopes() {
        return this.scopes;
    }

    /**
     * Gets the correlation ID for this authentication request.
     * 
     * @return correlation ID used to correlate requests and responses
     */
    public String getCorrelationId() {
        return this.correlationId;
    }

    /**
     * Gets the claims for this authentication request.
     * 
     * @return string containing one or multiple claims
     */
    public String getClaims() {
        return this.claims;
    }

    /**
     * Gets the tenant ID for this authentication request.
     * 
     * @return tenant ID
     */
    public String getTenantId() {
        return this.tenantId;
    }

    /**
     * Sets the scopes for this authentication request.
     * 
     * @param scopes set of scopes being requested
     */
    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    /**
     * Sets the correlation ID for this authentication request.
     * 
     * @param correlationId correlation ID used to correlate requests and responses
     */
    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    /**
     * Sets the claims for this authentication request.
     * 
     * @param claims string containing one or multiple claims
     */
    public void setClaims(String claims) {
        this.claims = claims;
    }

    /**
     * Sets the tenant ID for this authentication request.
     * 
     * @param tenantId tenant ID
     */
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
