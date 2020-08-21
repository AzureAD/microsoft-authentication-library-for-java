// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;

/**
 * Interface representing a single tenant profile. ITenantProfiles are made available through the
 * {@link IAccount#getTenantProfiles()} method of an Account
 *
 */
public interface ITenantProfile {

    /**
     * @return local OID
     */
    String getId();

    /**
     * @return local tenant ID
     */
    String getTenantId();

    /**
     * @return claims in id token
     */
    Map<String, ?> getClaims();

}
