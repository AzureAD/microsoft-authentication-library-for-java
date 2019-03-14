package com.microsoft.aad.msal4j;


import com.google.common.base.Strings;

import java.util.UUID;

class RequestContext {

    private String telemetryRequestId;
    private String clientId;
    private String correlationId;

    public RequestContext(String clientId, String correlationId){
        this.clientId = Strings.isNullOrEmpty(clientId) ? "unset_client_id" : clientId;
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

    static String generateNewCorrelationId(){
        return UUID.randomUUID().toString();
    }
}