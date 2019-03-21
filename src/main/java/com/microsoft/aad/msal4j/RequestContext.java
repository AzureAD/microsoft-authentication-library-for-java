package com.microsoft.aad.msal4j;


import com.google.common.base.Strings;

import java.util.UUID;

class RequestContext {

    private String telemetryRequestId;
    private String clientId;
    private String correlationId;
    private AcquireTokenPublicApi publicApi;

    public RequestContext(String clientId, String correlationId, AcquireTokenPublicApi publicApi){
        this.clientId = Strings.isNullOrEmpty(clientId) ? "unset_client_id" : clientId;
        this.publicApi= publicApi;
        this.correlationId = Strings.isNullOrEmpty(correlationId) ?
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

    public AcquireTokenPublicApi getAcquireTokenPublicApi(){
        return publicApi;
    }

    static String generateNewCorrelationId(){
        return UUID.randomUUID().toString();
    }
}