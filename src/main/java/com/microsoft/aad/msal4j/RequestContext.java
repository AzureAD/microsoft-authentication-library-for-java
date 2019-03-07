package com.microsoft.aad.msal4j;


import com.google.common.base.Strings;

class RequestContext {
    private String telemetryRequestId;
    private String clientId;

    public RequestContext(String clientId){
        this.clientId = Strings.isNullOrEmpty(clientId) ? "unset_client_id" : clientId;
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
}