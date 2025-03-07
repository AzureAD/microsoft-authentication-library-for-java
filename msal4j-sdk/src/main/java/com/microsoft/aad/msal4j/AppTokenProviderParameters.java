// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;

/// The authentication parameters provided to the app token provider callback.
public class AppTokenProviderParameters {

    /// Specifies which scopes to request.
    public Set<String> scopes;
    /// Correlation id of the authentication request.
    public String correlationId;
    /// A string with one or multiple claims.
    public String claims;
    /// tenant id
    public String tenantId;

    public AppTokenProviderParameters(Set<String> scopes, String correlationId, String claims, String tenantId) {
        this.scopes = scopes;
        this.correlationId = correlationId;
        this.claims = claims;
        this.tenantId = tenantId;
    }

    public Set<String> getScopes() {
        return this.scopes;
    }

    public String getCorrelationId() {
        return this.correlationId;
    }

    public String getClaims() {
        return this.claims;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public void setScopes(Set<String> scopes) {
        this.scopes = scopes;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public void setClaims(String claims) {
        this.claims = claims;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
