// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface representing a single tenant profile. ITenantProfiles are made available through the
 * {@link IAccount#getTenantProfiles()} method of an Account
 *
 */
public interface ITenantProfile extends Serializable {

    /**
     * A map of claims taken from an ID token. Keys and values will follow the structure of a JSON Web Token
     *
     * @return Map claims in id token
     */
    Map<String, ?> getClaims();

}
