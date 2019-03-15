package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.Secret;

import java.util.Set;

class UserNamePasswordRequest extends MsalRequest{

    UserNamePasswordRequest(String username,
                            String password,
                            Set<String> scopes,
                            ClientAuthentication clientAuthentication,
                            RequestContext requestContext) {
        super(createAuthenticationGrant(username, password, scopes), clientAuthentication, requestContext);
    }

    private static OauthAuthorizationGrant createAuthenticationGrant(
            String username,
            String password,
            Set<String> scopes ) {
        ResourceOwnerPasswordCredentialsGrant resourceOwnerPasswordCredentialsGrant =
                new ResourceOwnerPasswordCredentialsGrant(username, new Secret(password));

        return new OauthAuthorizationGrant(resourceOwnerPasswordCredentialsGrant, scopes);
    }
}
