// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.JWTBearerGrant;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Accessors(fluent = true)
@Getter
class OnBehalfOfRequest extends MsalRequest {

    OnBehalfOfRequest(OnBehalfOfParameters parameters,
                      ConfidentialClientApplication application,
                      RequestContext requestContext) {
        super(application, createAuthenticationGrant(parameters), requestContext);
    }

    private static OAuthAuthorizationGrant createAuthenticationGrant(OnBehalfOfParameters parameters) {

        AuthorizationGrant jWTBearerGrant;
        try {
            jWTBearerGrant = new JWTBearerGrant(SignedJWT.parse(parameters.userAssertion().getAssertion()));
        } catch (Exception e) {
            throw new AuthenticationException(e);
        }

        Map<String, List<String>> params = new HashMap<>();
        params.put("scope", Collections.singletonList(String.join(" ", parameters.scopes())));
        params.put("requested_token_use", Collections.singletonList("on_behalf_of"));

        return new OAuthAuthorizationGrant(jWTBearerGrant, params);
    }
}
