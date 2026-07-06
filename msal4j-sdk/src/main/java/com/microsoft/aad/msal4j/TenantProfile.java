// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Objects;

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

    public Map<String, ?> getClaims() {
        return idTokenClaims;
    }

    public Map<String, ?> idTokenClaims() {
        return this.idTokenClaims;
    }

    public String environment() {
        return this.environment;
    }

    public TenantProfile idTokenClaims(Map<String, ?> idTokenClaims) {
        this.idTokenClaims = idTokenClaims;
        return this;
    }

    public TenantProfile environment(String environment) {
        this.environment = environment;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantProfile)) return false;

        TenantProfile other = (TenantProfile) o;

        return Objects.equals(idTokenClaims, other.idTokenClaims)
                && Objects.equals(environment, other.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idTokenClaims, environment);
    }
}
