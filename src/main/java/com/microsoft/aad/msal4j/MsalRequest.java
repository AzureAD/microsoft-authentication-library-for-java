package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;

abstract class MsalRequest {
    private final ClientAuthentication clientAuthentication;
    private AbstractMsalAuthorizationGrant msalAuthorizationGrant;
    private RequestContext requestContext;
    ClientDataHttpHeaders headers;

    protected MsalRequest(AbstractMsalAuthorizationGrant msalAuthorizationGrant,
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

    AbstractMsalAuthorizationGrant getMsalAuthorizationGrant(){
        return msalAuthorizationGrant;
    }

    RequestContext getRequestContext(){
        return requestContext;
    }

    ClientDataHttpHeaders getHeaders(){
        return headers;
    }

    void setMsalAuthorizationGrant(AbstractMsalAuthorizationGrant msalAuthorizationGrant){
        this.msalAuthorizationGrant = msalAuthorizationGrant;
    }



}


