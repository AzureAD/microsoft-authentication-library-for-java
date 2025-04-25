// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class UserNamePasswordRequest extends MsalRequest {

    UserNamePasswordRequest(UserNamePasswordParameters parameters,
                            PublicClientApplication application,
                            RequestContext requestContext) {
        super(application, createAuthenticationGrant(parameters), requestContext);
    }

    private static OAuthAuthorizationGrant createAuthenticationGrant(UserNamePasswordParameters parameters) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, Collections.singletonList(GrantConstants.PASSWORD));
        params.put(GrantConstants.USERNAME_PARAMETER, Collections.singletonList(parameters.username()));
        params.put(GrantConstants.PASSWORD_PARAMETER, Collections.singletonList(new String(parameters.password())));

        return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
    }
}
