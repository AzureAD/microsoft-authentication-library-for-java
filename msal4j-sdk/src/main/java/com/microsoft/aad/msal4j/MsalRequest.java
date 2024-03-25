// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
@AllArgsConstructor
abstract class MsalRequest {

    AbstractMsalAuthorizationGrant msalAuthorizationGrant;

    private final AbstractApplicationBase application;

    private final RequestContext requestContext;

    @Getter(value = AccessLevel.PACKAGE, lazy = true)
    private final HttpHeaders headers = new HttpHeaders(requestContext);

    MsalRequest(AbstractApplicationBase clientApplicationBase, AbstractMsalAuthorizationGrant abstractMsalAuthorizationGrant, RequestContext requestContext) {
        this.application = clientApplicationBase;
        this.msalAuthorizationGrant = abstractMsalAuthorizationGrant;
        this.requestContext = requestContext;

        CurrentRequest currentRequest = new CurrentRequest(requestContext.publicApi());
        application.serviceBundle().getServerSideTelemetry().setCurrentRequest(currentRequest);
    }

    MsalRequest(AbstractApplicationBase clientApplicationBase, RequestContext requestContext) {
        this.application = clientApplicationBase;
        this.requestContext = requestContext;

        CurrentRequest currentRequest = new CurrentRequest(requestContext.publicApi());
        application.serviceBundle().getServerSideTelemetry().setCurrentRequest(currentRequest);
    }

    MsalRequest() {
        this.application = null;
        this.requestContext = null;
    }
}
