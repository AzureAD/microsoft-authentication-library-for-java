// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract class for an MSAL grant.
 */
abstract class AbstractMsalAuthorizationGrant {

    /**
     * Converts the grant into a HTTP parameters map.
     *
     * @return A map contains the HTTP parameters
     */
    abstract Map<String, String> toParameters();

    static final String SCOPE_PARAM_NAME = "scope";
    static final String SCOPES_DELIMITER = " ";

    static final String SCOPE_OPEN_ID = "openid";
    static final String SCOPE_PROFILE = "profile";
    static final String SCOPE_OFFLINE_ACCESS = "offline_access";

    static final Set<String> COMMON_SCOPES = Stream.of(SCOPE_OPEN_ID, SCOPE_PROFILE, SCOPE_OFFLINE_ACCESS)
            .collect(Collectors.toCollection(HashSet::new));

    Set<String> scopes;

    Set<String> getScopes() {
        return scopes;
    }

    ClaimsRequest claims;

    ClaimsRequest getClaims() {
        return claims;
    }
}
