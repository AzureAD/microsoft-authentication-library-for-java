// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.util.Map;

/**
 * Interface representing a single user account. An IAccount is returned in the {@link IAuthenticationResult}
 * property, and is used as parameter in {@link SilentParameters#builder(Set, IAccount)} )}
 *
 */
public interface IAccount extends Serializable {

    /**
     * @return account id
     */
    String homeAccountId();

    /**
     * @return account environment
     */
    String environment();

    /**
     * @return account username
     */
    String username();

    /**
     * Map of {@link ITenantProfile} objects related to this account, the keys of the map are the tenant ID values and
     * match the 'realm' key of an ID token
     *
     * @return tenant profiles
     */
    Map<String, ITenantProfile> getTenantProfiles();
}
