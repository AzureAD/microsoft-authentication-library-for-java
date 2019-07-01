// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.List;
import java.util.Map;
import java.util.Set;

class IntegratedWindowsAuthorizationGrant extends AbstractMsalAuthorizationGrant {

    private final String userName;

    IntegratedWindowsAuthorizationGrant(Set<String> scopes, String userName) {
        this.userName = userName;
        this.scopes = String.join(" ", scopes);
    }

    @Override
    Map<String, List<String>> toParameters() {
        return null;
    }

    String getUserName() {
        return userName;
    }
}
