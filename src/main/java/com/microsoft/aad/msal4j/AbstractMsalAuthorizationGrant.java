// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;

/**
 * Abstract class for an MSAL grant.
 */
abstract class AbstractMsalAuthorizationGrant {

    /**
     *  Converts the grant into a HTTP parameters map.
     *
     * @return A map contains the HTTP parameters
     */
    abstract Map<String, List<String>> toParameters();

    static final String SCOPE_PARAM_NAME = "scope";
    static final String SCOPES_DELIMITER = " ";

    static final String SCOPE_OPEN_ID = "openid";
    static final String SCOPE_PROFILE = "profile";
    static final String SCOPE_OFFLINE_ACCESS = "offline_access";

    static final String COMMON_SCOPES_PARAM = SCOPE_OPEN_ID + SCOPES_DELIMITER +
            SCOPE_PROFILE + SCOPES_DELIMITER +
            SCOPE_OFFLINE_ACCESS;

    String scopes;

    String getScopes() {
        return scopes;
    }
}
