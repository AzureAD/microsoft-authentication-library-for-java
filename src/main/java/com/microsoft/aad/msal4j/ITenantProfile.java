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
     * This value corresponds to the 'oid' key of an ID token
     *
     * @return String local OID
     */
    String getId();

    /**
     * This value corresponds to the 'realm' key of an ID token
     *
     * @return String local tenant ID
     */
    String getTenantId();

    /**
     * @return Map claims in id token
     */
    Map<String, ?> getClaims();

}
