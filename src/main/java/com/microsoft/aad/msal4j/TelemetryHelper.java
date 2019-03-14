package com.microsoft.aad.msal4j;

class TelemetryHelper implements AutoCloseable{

    private Event eventToEnd;
    private String requestId;
    private String clientId;
    private ITelemetry telemetry;

    TelemetryHelper(ITelemetry telemetry,
                    String requestId,
                    String clientId,
                    Event event) {

        this.telemetry = telemetry;
        this.requestId = requestId;
        this.clientId = clientId;
        this.eventToEnd = event;

        if(telemetry != null){
            telemetry.startEvent(requestId, event);
        }
    }

    public void close(){
        if(telemetry != null) {
            telemetry.stopEvent(requestId, eventToEnd);
            telemetry.flush(requestId, clientId);
        }
    }

}
