package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.AuthorizationCode;
import com.nimbusds.oauth2.sdk.AuthorizationCodeGrant;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.net.URI;
import java.util.Set;

class AuthorizationCodeRequest extends MsalRequest {

    AuthorizationCodeRequest(Set<String> scopes,
                             String authorizationCode,
                             URI redirectUri,
                             ClientAuthentication clientAuthentication,
                             RequestContext requestContext){
        super(createMsalGrant(authorizationCode, redirectUri, scopes), clientAuthentication, requestContext);
    }

    private static MsalAuthorizationGrant createMsalGrant(
            String authorizationCode,
            URI redirectUri,
            Set<String> scopes){

        AuthorizationGrant authorizationGrant = new AuthorizationCodeGrant(
                new AuthorizationCode(authorizationCode), redirectUri);

        return new OauthAuthorizationGrant(authorizationGrant, scopes);
    }
}
