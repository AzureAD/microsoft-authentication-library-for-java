// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.UUID;

@Accessors(fluent = true)
@Getter(AccessLevel.PACKAGE)
class RequestContext {

    @Setter(AccessLevel.PACKAGE)
    private String telemetryRequestId;
    private String clientId;
    private String correlationId;
    private PublicApi publicApi;
    private String applicationName;
    private String applicationVersion;

    public RequestContext(ClientApplicationBase clientApplication, PublicApi publicApi){
        this.clientId = StringHelper.isBlank(clientApplication.clientId()) ?
                "unset_client_id" :
                clientApplication.clientId();
        this.applicationName = StringHelper.isBlank(clientApplication.applicationName()) ?
                "" :
                clientApplication.applicationName();

        this.applicationVersion = StringHelper.isBlank(clientApplication.applicationVersion()) ?
                "" :
                clientApplication.applicationVersion();

        this.correlationId = StringHelper.isBlank(clientApplication.correlationId()) ?
                generateNewCorrelationId() :
                clientApplication.correlationId();

        this.publicApi = publicApi;
    }

    static String generateNewCorrelationId(){
        return UUID.randomUUID().toString();
    }
}