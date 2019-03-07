package com.microsoft.aad.msal4j;

interface ITelemetryManager {
    String generateRequestId();
    TelemetryHelper createTelemetryHelper(String requestId, String clientId, Event event, boolean shouldFlush);
}
