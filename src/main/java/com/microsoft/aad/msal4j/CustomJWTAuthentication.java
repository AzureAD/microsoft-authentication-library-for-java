package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.id.ClientID;

public class CustomJWTAuthentication extends ClientAuthentication {

    protected CustomJWTAuthentication(ClientAuthenticationMethod method, ClientID clientID) {
        super(method, clientID);
    }

    @Override
    public void applyTo(HTTPRequest httpRequest) {

    }
}
