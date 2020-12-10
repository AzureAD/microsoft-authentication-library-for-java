// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.auth.Secret;

class UserNamePasswordRequest extends MsalRequest{

    UserNamePasswordRequest(UserNamePasswordParameters parameters,
                            PublicClientApplication application,
                            RequestContext requestContext) {
        super(application, createAuthenticationGrant(parameters), requestContext);
    }

    private static OAuthAuthorizationGrant createAuthenticationGrant(
            UserNamePasswordParameters parameters) {

        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant =
                new ResourceOwnerPasswordCredentialsGrant(parameters.username(),
                        new Secret(new String(parameters.password())));

        return new OAuthAuthorizationGrant(resourceOwnerPasswordCredentialsGrant, parameters.scopes(), parameters.claims());
    }
}
