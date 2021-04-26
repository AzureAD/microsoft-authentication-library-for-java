// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import java.util.Map;

/**
 * Representation of a single tenant profile
 */
@Accessors(fluent = true)
@Getter
@Setter
@AllArgsConstructor
class TenantProfile implements ITenantProfile {

    Map<String, ?> idTokenClaims;

    String environment;

    public Map<String, ?> getClaims() {
        return idTokenClaims;
    }
}
