// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.JWTBearerGrant;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.microsoft.aad.msal4j.AbstractMsalAuthorizationGrant.SCOPES_DELIMITER;

class OnBehalfOfRequest extends MsalRequest {

    OnBehalfOfParameters parameters;

    OnBehalfOfRequest(OnBehalfOfParameters parameters,
                      ConfidentialClientApplication application,
                      RequestContext requestContext) {
        super(application, createAuthenticationGrant(parameters), requestContext);
        this.parameters = parameters;
    }

    private static OAuthAuthorizationGrant createAuthenticationGrant(OnBehalfOfParameters parameters) {
        Map<String, List<String>> params = new LinkedHashMap<>();

        params.put(GrantConstants.GRANT_TYPE_PARAMETER, Collections.singletonList(GrantConstants.JWT_BEARER));
        params.put(GrantConstants.ASSERTION_PARAMETER, Collections.singletonList(parameters.userAssertion().getAssertion()));
        params.put("requested_token_use", Collections.singletonList("on_behalf_of"));

        if (parameters.claims() != null) {
            params.put("claims", Collections.singletonList(parameters.claims().formatAsJSONString()));
        }

        return new OAuthAuthorizationGrant(params, parameters.scopes(), null);
    }

    OnBehalfOfParameters parameters() {
        return this.parameters;
    }
}
