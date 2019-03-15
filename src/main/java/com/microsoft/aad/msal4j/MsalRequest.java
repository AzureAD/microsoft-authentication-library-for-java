package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

abstract class MsalRequest {

    private final ClientAuthentication clientAuthentication;
    private MsalAuthorizationGrant msalAuthorizationGrant;
    private final RequestContext requestContext;
    ClientDataHttpHeaders headers;

    protected MsalRequest(MsalAuthorizationGrant msalAuthorizationGrant,
                          ClientAuthentication clientAuthentication,
                          RequestContext requestContext){
        this.msalAuthorizationGrant = msalAuthorizationGrant;
        this.clientAuthentication = clientAuthentication;
        this.requestContext = requestContext;
        this.headers = new ClientDataHttpHeaders(requestContext.getCorrelationId());
    }

    ClientAuthentication getClientAuthentication() {
        return clientAuthentication;
    }

    MsalAuthorizationGrant getMsalAuthorizationGrant(){
        return msalAuthorizationGrant;
    }

    RequestContext getRequestContext(){
        return requestContext;
    }

    ClientDataHttpHeaders getHeaders(){
        return headers;
    }

    void setMsalAuthorizationGrant(MsalAuthorizationGrant msalAuthorizationGrant){
        this.msalAuthorizationGrant = msalAuthorizationGrant;
    }
}


