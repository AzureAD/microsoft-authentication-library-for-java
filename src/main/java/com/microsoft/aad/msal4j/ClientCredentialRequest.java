package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ClientCredentialsGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

import java.util.Set;

public class ClientCredentialRequest extends MsalRequest{
    ClientCredentialRequest(Set<String> scopes,
                            ClientAuthentication clientAuthentication,
                            RequestContext requestContext){
        super(createMsalGrant(scopes), clientAuthentication, requestContext );
    }

    private static MsalOAuthAuthorizationGrant createMsalGrant(Set<String> scopes){

        return new MsalOAuthAuthorizationGrant(new ClientCredentialsGrant(), scopes);
    }

}
