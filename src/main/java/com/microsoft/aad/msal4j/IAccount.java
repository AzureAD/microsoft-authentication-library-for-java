// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Map;
import java.util.Set;

/**
 * Interface representing a single user account. An IAccount is returned in the {@link IAuthenticationResult}
 * property, and is used as parameter in {@link SilentParameters#builder(Set, IAccount)} )}
 *
 */
public interface IAccount {

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
     * @return claims in id token
     */
    Map<String, ?> getClaims();

    /**
     * @return tenant id
     */
    String getTenantId();

    /**
     * @return tenant profiles
     */
    Map<String, IAccount> getTenantProfiles();
}
