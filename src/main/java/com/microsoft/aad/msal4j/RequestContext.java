// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.UUID;

class RequestContext {

    private String telemetryRequestId;
    private String clientId;
    private String correlationId;
    private PublicApi publicApi;

    public RequestContext(String clientId, String correlationId, PublicApi publicApi){
        this.clientId = StringHelper.isBlank(clientId) ? "unset_client_id" : clientId;
        this.publicApi= publicApi;
        this.correlationId = StringHelper.isBlank(correlationId) ?
                generateNewCorrelationId() :
                correlationId;
    }

    public String getTelemetryRequestId() {
        return telemetryRequestId;
    }

    public void setTelemetryRequestId(String telemetryRequestId) {
        this.telemetryRequestId = telemetryRequestId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getCorrelationId(){
        return correlationId;
    }

    public PublicApi getAcquireTokenPublicApi(){
        return publicApi;
    }

    static String generateNewCorrelationId(){
        return UUID.randomUUID().toString();
    }
}