package com.microsoft.aad.msal4j;

import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

class ServerSideTelemetry {

    private final static String SCHEMA_VERSION = "2";
    private final static String SCHEMA_PIPE_DELIMITER = "|";
    private final static String SCHEMA_COMMA_DELIMITER = ",";
    private final static String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    private final static String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    private CurrentRequest currentRequest;

    static AtomicInteger silentSuccessfulCount = new AtomicInteger(0);
    static ConcurrentMap<String, String[]> previousRequests = new ConcurrentHashMap<>();

    synchronized Map<String, String> getServerTelemetryHeaderMap(){
        Map<String, String> headerMap = new HashMap<>();

        headerMap.put(CURRENT_REQUEST_HEADER_NAME, buildCurrentRequestHeader());
        headerMap.put(LAST_REQUEST_HEADER_NAME, buildLastRequestHeader());

        return headerMap;
    }

    static void addFailedRequestTelemetry(String publicApiId, String correlationId, String error){

        String[] previousRequest = new String[]{publicApiId, error};
        previousRequests.put(
                correlationId,
                previousRequest);
    }

    static void incrementSilentSuccessfulCount(){
        silentSuccessfulCount.incrementAndGet();
    }

    synchronized CurrentRequest getCurrentRequest() {
        return currentRequest;
    }

    synchronized void setCurrentRequest(CurrentRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    private synchronized String buildCurrentRequestHeader(){
        if(currentRequest == null){
            return StringHelper.EMPTY_STRING;
        }

        return SCHEMA_VERSION +
                SCHEMA_PIPE_DELIMITER +
                currentRequest.publicApi().getApiId() +
                SCHEMA_COMMA_DELIMITER +
                currentRequest.forceRefresh() +
                SCHEMA_PIPE_DELIMITER;
    }

    private synchronized String buildLastRequestHeader() {

        // LastRequest header schema:
        // schema_version|silent_successful_count|api_id1,correlation_id1|error1|
        StringBuilder lastRequestBuilder = new StringBuilder();

        lastRequestBuilder
                .append(SCHEMA_VERSION)
                .append(SCHEMA_PIPE_DELIMITER)
                .append(silentSuccessfulCount.getAndSet(0));

        if (previousRequests.isEmpty()) {
            // Kusto queries always expect all delimiters so return
            // "schema_version|silent_successful_count|||"
            return lastRequestBuilder
                    .append(SCHEMA_PIPE_DELIMITER)
                    .append(SCHEMA_PIPE_DELIMITER)
                    .append(SCHEMA_PIPE_DELIMITER)
                    .toString();
        }

        StringBuilder middleSegmentBuilder = new StringBuilder(SCHEMA_PIPE_DELIMITER);
        StringBuilder errorSegmentBuilder = new StringBuilder(SCHEMA_PIPE_DELIMITER);

        Iterator<String> it = previousRequests.keySet().iterator();

        // Total header size should be less than 8kb. At max, we will use 4kb for telemetry.
        while (it.hasNext()
                && (middleSegmentBuilder.length() + errorSegmentBuilder.length()) < 3800)
        {
            String correlationId = it.next();
            String[] previousRequest = previousRequests.get(correlationId);
            String apiId = (String)Array.get(previousRequest, 0);
            String error = (String)Array.get(previousRequest, 1);

            middleSegmentBuilder.append(apiId).append(SCHEMA_COMMA_DELIMITER).append(correlationId);
            errorSegmentBuilder.append(error);

            if(it.hasNext()){
                middleSegmentBuilder.append(SCHEMA_COMMA_DELIMITER);
                errorSegmentBuilder.append(SCHEMA_COMMA_DELIMITER);
            }

            it.remove();
        }

        errorSegmentBuilder.append(SCHEMA_PIPE_DELIMITER);

        return lastRequestBuilder.append(middleSegmentBuilder).append(errorSegmentBuilder).toString();
    }
}
