// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

class ServerSideTelemetry {

    private final static String SCHEMA_VERSION = "1";
    private final static String SCHEMA_PIPE_DELIMITER = "|";
    private final static String SCHEMA_COMMA_DELIMITER = ",";
    private final static String EMPTY_STRING = "";
    private final static String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    private final static String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    private CurrentRequest currentRequest;
    private LastRequest lastRequest;

    synchronized Map<String, String> getServerTelemetryHeaderMap(){
        Map<String, String> headerMap = new HashMap<>();

        headerMap.put(CURRENT_REQUEST_HEADER_NAME, buildCurrentRequestHeader());
        headerMap.put(LAST_REQUEST_HEADER_NAME, buildLastRequestHeader());

        return headerMap;
    }

    private String buildCurrentRequestHeader(){
        if(currentRequest == null){
            return EMPTY_STRING;
        }

        return SCHEMA_VERSION +
                SCHEMA_PIPE_DELIMITER +
                currentRequest.publicApi().getApiId() +
                SCHEMA_COMMA_DELIMITER +
                currentRequest.forceRefresh() +
                SCHEMA_PIPE_DELIMITER;
    }

    private String buildLastRequestHeader(){
        if(lastRequest == null){
            return EMPTY_STRING;
        }

        String errorCode = StringHelper.isBlank(lastRequest.errorCode()) ?
                EMPTY_STRING :
                lastRequest.errorCode();

        return SCHEMA_VERSION +
                SCHEMA_PIPE_DELIMITER +
                lastRequest.publicApi().getApiId() +
                SCHEMA_COMMA_DELIMITER +
                lastRequest.correlationId() +
                SCHEMA_COMMA_DELIMITER +
                errorCode +
                SCHEMA_PIPE_DELIMITER;
    }

    synchronized CurrentRequest getCurrentRequest() {
        return currentRequest;
    }

    synchronized void setCurrentRequest(CurrentRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    synchronized LastRequest getLastRequest() {
        return lastRequest;
    }

    synchronized void setLastRequest(LastRequest lastRequest) {
        this.lastRequest = lastRequest;
    }
}
