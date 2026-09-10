// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

abstract class MsalRequest {

    AbstractMsalAuthorizationGrant msalAuthorizationGrant;
    private final AbstractApplicationBase application;
    private final RequestContext requestContext;
    private final HttpHeaders headers;
    private String extCacheKeyHash;

    MsalRequest(AbstractApplicationBase clientApplicationBase, AbstractMsalAuthorizationGrant abstractMsalAuthorizationGrant, RequestContext requestContext) {
        this.application = clientApplicationBase;
        this.msalAuthorizationGrant = abstractMsalAuthorizationGrant;
        this.requestContext = requestContext;
        this.headers = new HttpHeaders(requestContext);

        CurrentRequest currentRequest = new CurrentRequest(requestContext.publicApi());
        application.serviceBundle().getServerSideTelemetry().setCurrentRequest(currentRequest);
    }

    MsalRequest(AbstractApplicationBase clientApplicationBase, RequestContext requestContext) {
        this.application = clientApplicationBase;
        this.requestContext = requestContext;
        this.headers = new HttpHeaders(requestContext);

        CurrentRequest currentRequest = new CurrentRequest(requestContext.publicApi());
        application.serviceBundle().getServerSideTelemetry().setCurrentRequest(currentRequest);
    }

    AbstractMsalAuthorizationGrant msalAuthorizationGrant() {
        return this.msalAuthorizationGrant;
    }

    AbstractApplicationBase application() {
        return this.application;
    }

    RequestContext requestContext() {
        return this.requestContext;
    }

    HttpHeaders headers() {
        return this.headers;
    }

    String extCacheKeyHash() {
        return extCacheKeyHash;
    }

    void extCacheKeyHash(String extCacheKeyHash) {
        this.extCacheKeyHash = extCacheKeyHash;
    }
}
