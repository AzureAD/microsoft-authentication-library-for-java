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
    private String authority;
    private IApiParameters apiParameters;
    private IClientApplicationBase clientApplication;

    public RequestContext(AbstractClientApplicationBase clientApplication,
                          PublicApi publicApi,
                          IApiParameters apiParameters) {
        this.clientApplication = clientApplication;

        this.clientId = StringHelper.isBlank(clientApplication.clientId()) ?
                "unset_client_id" :
                clientApplication.clientId();
        this.correlationId = StringHelper.isBlank(clientApplication.correlationId()) ?
                generateNewCorrelationId() :
                clientApplication.correlationId();

        this.applicationVersion = clientApplication.applicationVersion();
        this.applicationName = clientApplication.applicationName();
        this.publicApi = publicApi;
        this.authority = clientApplication.authority();
        this.apiParameters = apiParameters;
    }

    private static String generateNewCorrelationId() {
        return UUID.randomUUID().toString();
    }
}