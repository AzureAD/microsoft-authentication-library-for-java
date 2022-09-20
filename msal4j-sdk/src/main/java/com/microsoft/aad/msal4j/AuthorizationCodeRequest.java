// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.pkce.CodeVerifier;

class AuthorizationCodeRequest extends MsalRequest {

    AuthorizationCodeRequest(AuthorizationCodeParameters parameters,
                             AbstractClientApplicationBase application,
                             RequestContext requestContext) {
        super(application, createMsalGrant(parameters), requestContext);
    }

    private static AbstractMsalAuthorizationGrant createMsalGrant(AuthorizationCodeParameters parameters) {

        AuthorizationGrant authorizationGrant;
        if (parameters.codeVerifier() != null) {
            authorizationGrant = new AuthorizationCodeGrant(
                    new AuthorizationCode(parameters.authorizationCode()),
                    parameters.redirectUri(),
                    new CodeVerifier(parameters.codeVerifier()));

        } else {
            authorizationGrant = new AuthorizationCodeGrant(
                    new AuthorizationCode(parameters.authorizationCode()), parameters.redirectUri());
        }

        return new OAuthAuthorizationGrant(authorizationGrant, parameters.scopes(), parameters.claims());
    }
}
