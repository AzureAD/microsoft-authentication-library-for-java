package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.RefreshTokenGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.token.RefreshToken;

import java.util.Set;

public class RefreshTokenRequest extends MsalRequest {

    RefreshTokenRequest(String refreshToken,
                        Set<String> scopes,
                        ClientAuthentication clientAuthentication,
                        RequestContext requestContext){
        super(createAuthenticationGrant(refreshToken, scopes), clientAuthentication, requestContext);
    }

    private static AbstractMsalAuthorizationGrant createAuthenticationGrant(String refreshToken,
                                                                            Set<String > scopes){
        RefreshTokenGrant refreshTokenGrant = new RefreshTokenGrant(new RefreshToken(refreshToken));
        return new MsalOAuthAuthorizationGrant(refreshTokenGrant, scopes);
    }
}
