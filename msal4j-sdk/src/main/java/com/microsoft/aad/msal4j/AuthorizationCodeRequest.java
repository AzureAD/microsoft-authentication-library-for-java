// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.LinkedHashMap;
import java.util.Map;

class AuthorizationCodeRequest extends MsalRequest {

    AuthorizationCodeRequest(AuthorizationCodeParameters parameters,
                             AbstractClientApplicationBase application,
                             RequestContext requestContext) {
        super(application, createMsalGrant(parameters), requestContext);
    }

    private static AbstractMsalAuthorizationGrant createMsalGrant(AuthorizationCodeParameters parameters) {
        Map<String, String> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, GrantConstants.AUTHORIZATION_CODE);
        params.put("code", parameters.authorizationCode());

        if (parameters.redirectUri() != null) {
            params.put("redirect_uri", parameters.redirectUri().toString());
        }

        if (parameters.codeVerifier() != null) {
            params.put("code_verifier", parameters.codeVerifier());
        }

        return new OAuthAuthorizationGrant(params, parameters.scopes(), parameters.claims());
    }
}
