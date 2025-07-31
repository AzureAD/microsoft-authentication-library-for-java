// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;

/**
 * Representation of a single tenant profile
 */
class TenantProfile implements ITenantProfile {

    Map<String, ?> idTokenClaims;

    String environment;

    public TenantProfile(Map<String, ?> idTokenClaims, String environment) {
        this.idTokenClaims = idTokenClaims;
        this.environment = environment;
    }

    /**
     * Gets the claims.
     * 
     * @return the claims
     */
    public Map<String, ?> getClaims() {
        return idTokenClaims;
    }

    /**
     * TODO: Add description
     */
    public Map<String, ?> idTokenClaims() {
        return this.idTokenClaims;
    }

    /**
     * TODO: Add description
     */
    public String environment() {
        return this.environment;
    }

    /**
     * TODO: Add description
     */
    public TenantProfile idTokenClaims(Map<String, ?> idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
        return this;
    }

    /**
     * TODO: Add description
     */
    public TenantProfile environment(String environment) {
        this.environment = environment;
        return this;
    }
}
