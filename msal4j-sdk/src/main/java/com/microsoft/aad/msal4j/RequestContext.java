// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.UUID;

class RequestContext {

    private String telemetryRequestId;
    private String clientId;
    private String correlationId;
    private PublicApi publicApi;
    private String applicationName;
    private String applicationVersion;
    private String authority;
    private IAcquireTokenParameters apiParameters;
    private IApplicationBase clientApplication;
    private UserIdentifier userIdentifier;
    private final AgeAssuranceTargetValidation ageAssuranceTargetValidation;

    public RequestContext(AbstractApplicationBase clientApplication,
                          PublicApi publicApi,
                          IAcquireTokenParameters apiParameters) {
        this(clientApplication, publicApi, apiParameters, null,
                AgeAssuranceTargetValidation.fromParameters(apiParameters));
    }

    RequestContext(AbstractApplicationBase clientApplication,
                   PublicApi publicApi,
                   IAcquireTokenParameters apiParameters,
                   UserIdentifier userIdentifier,
                   AgeAssuranceTargetValidation ageAssuranceTargetValidation) {
        this.clientApplication = clientApplication;
        this.ageAssuranceTargetValidation = ageAssuranceTargetValidation;
        this.userIdentifier = userIdentifier;

        this.clientId = StringHelper.isBlank(clientApplication.clientId()) ?
                "unset_client_id" :
                clientApplication.clientId();
        this.correlationId = StringHelper.isBlank(clientApplication.correlationId()) ?
                generateNewCorrelationId() :
                clientApplication.correlationId();

        if (clientApplication instanceof AbstractClientApplicationBase) {
            this.applicationVersion = ((AbstractClientApplicationBase) clientApplication).applicationVersion();
            this.applicationName = ((AbstractClientApplicationBase) clientApplication).applicationName();
        }
        this.publicApi = publicApi;
        this.authority = clientApplication.authority();
        this.apiParameters = apiParameters;
    }

    public RequestContext(AbstractApplicationBase clientApplication,
                          PublicApi publicApi,
                          IAcquireTokenParameters apiParameters,
                          UserIdentifier userIdentifier) {
        this(clientApplication, publicApi, apiParameters);
        this.userIdentifier = userIdentifier;
    }

    private static String generateNewCorrelationId() {
        return UUID.randomUUID().toString();
    }

    String telemetryRequestId() {
        return this.telemetryRequestId;
    }

    String clientId() {
        return this.clientId;
    }

    String correlationId() {
        return this.correlationId;
    }

    PublicApi publicApi() {
        return this.publicApi;
    }

    String applicationName() {
        return this.applicationName;
    }

    String applicationVersion() {
        return this.applicationVersion;
    }

    String authority() {
        return this.authority;
    }

    IAcquireTokenParameters apiParameters() {
        return this.apiParameters;
    }

    AgeAssuranceTargetValidation ageAssuranceTargetValidation() {
        return ageAssuranceTargetValidation;
    }

    IApplicationBase clientApplication() {
        return this.clientApplication;
    }

    UserIdentifier userIdentifier() {
        return this.userIdentifier;
    }

    RequestContext telemetryRequestId(String telemetryRequestId) {
        this.telemetryRequestId = telemetryRequestId;
        return this;
    }
}