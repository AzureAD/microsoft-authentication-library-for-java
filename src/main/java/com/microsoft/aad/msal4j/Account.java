// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.util.Map;

/**
 * Representation of a single user account. If modifying this object, ensure it is compliant with
 * cache persistent model
 */
@Accessors(fluent = true)
@Getter
@Setter
@AllArgsConstructor
class Account implements IAccount {

    String homeAccountId;

    String environment;

    String username;

    String localAccountId;

    Map<String, ?> idTokenClaims;

    Map<String, IAccount> tenantProfiles;

    public String getTenantId() {
        return localAccountId;
    }

    public Map<String, ?> getClaims() {
        return idTokenClaims;
    }

    public Map<String, IAccount> getTenantProfiles() {
        return tenantProfiles;
    }
}
