package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.util.Set;

class IntegratedWindowsAuthRequest extends MsalRequest{

    IntegratedWindowsAuthRequest(String username,
                                 Set<String> scopes,
                                 ClientAuthentication clientAuthentication,
                                 RequestContext requestContext){
            super(createAuthenticationGrant(username, scopes), clientAuthentication, requestContext);
    }

    private static AbstractMsalAuthorizationGrant createAuthenticationGrant(String username,
                                                                            Set<String> scopes){
        return new MsalIntegratedAuthorizationGrant(username, scopes);
    }
}
