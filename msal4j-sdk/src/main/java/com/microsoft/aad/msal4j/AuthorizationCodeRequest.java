// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class AuthorizationCodeRequest extends MsalRequest {

    AuthorizationCodeRequest(AuthorizationCodeParameters parameters,
                             AbstractClientApplicationBase application,
                             RequestContext requestContext) {
        super(application, createMsalGrant(parameters), requestContext);
    }

    private static AbstractMsalAuthorizationGrant createMsalGrant(AuthorizationCodeParameters parameters) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, Collections.singletonList(GrantConstants.AUTHORIZATION_CODE));
        params.put("code", Collections.singletonList(parameters.authorizationCode()));

        if (parameters.redirectUri() != null) {
            params.put("redirect_uri", Collections.singletonList(parameters.redirectUri().toString()));
        }

        if (parameters.codeVerifier() != null) {
            params.put("code_verifier", Collections.singletonList(parameters.codeVerifier()));
        }

        return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
    }
}
