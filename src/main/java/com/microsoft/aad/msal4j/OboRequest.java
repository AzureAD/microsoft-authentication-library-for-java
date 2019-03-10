package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.JWTBearerGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.util.Set;

class OboRequest extends MsalRequest {
    OboRequest(UserAssertion userAssertion,
               Set<String> scopes,
               ClientAuthentication clientAuthentication,
               RequestContext requestContext){
        super(createAuthenticationGrant(userAssertion, scopes), clientAuthentication, requestContext);
    }

    private static MsalOAuthAuthorizationGrant createAuthenticationGrant(
            UserAssertion userAssertion,
            Set<String> scopes){

        AuthorizationGrant jWTBearerGrant;
        try{
           jWTBearerGrant = new JWTBearerGrant(SignedJWT.parse(userAssertion.getAssertion()));
        }catch(Exception e){
            throw new AuthenticationException(e);
        }
        return new MsalOAuthAuthorizationGrant(jWTBearerGrant, scopes);
    }
}
