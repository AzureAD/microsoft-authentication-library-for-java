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

    private final ClientApplicationBase application;

    private final RequestContext requestContext;

    @Getter(lazy = true)
    private final ClientDataHttpHeaders headers = new ClientDataHttpHeaders(requestContext.getCorrelationId());

    MsalRequest(ClientApplicationBase clientApplicationBase,
                AbstractMsalAuthorizationGrant abstractMsalAuthorizationGrant,
                RequestContext requestContext){

        this.application = clientApplicationBase;
        this.msalAuthorizationGrant = abstractMsalAuthorizationGrant;
        this.requestContext = requestContext;

        CurrentRequest currentRequest = new CurrentRequest(requestContext.getAcquireTokenPublicApi());
        application.getServiceBundle().getServerSideTelemetry().setCurrentRequest(currentRequest);
    }


}


