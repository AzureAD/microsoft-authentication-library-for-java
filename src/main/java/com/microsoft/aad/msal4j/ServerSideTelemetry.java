package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

class ServerSideTelemetry {

    private final static Logger log = LoggerFactory.getLogger(ServerSideTelemetry.class);

    private final static String SCHEMA_VERSION = "2";
    private final static String SCHEMA_PIPE_DELIMITER = "|";
    private final static String SCHEMA_COMMA_DELIMITER = ",";
    private final static String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    private final static String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    private CurrentRequest currentRequest;
    private AtomicInteger silentSuccessfulCount = new AtomicInteger(0);

    ConcurrentMap<String, String[]> previousRequests = new ConcurrentHashMap<>();
    ConcurrentMap<String, String[]> previousRequestInProgress = new ConcurrentHashMap<>();

    synchronized Map<String, String> getServerTelemetryHeaderMap() {
        Map<String, String> headerMap = new HashMap<>();

        headerMap.put(CURRENT_REQUEST_HEADER_NAME, buildCurrentRequestHeader());
        headerMap.put(LAST_REQUEST_HEADER_NAME, buildLastRequestHeader());

        return headerMap;
    }

    void addFailedRequestTelemetry(String publicApiId, String correlationId, String error) {

        String[] previousRequest = new String[]{publicApiId, error};
        previousRequests.put(
                correlationId,
                previousRequest);
    }

    void incrementSilentSuccessfulCount() {
        silentSuccessfulCount.incrementAndGet();
    }

    synchronized CurrentRequest getCurrentRequest() {
        return currentRequest;
    }

    synchronized void setCurrentRequest(CurrentRequest currentRequest) {
        this.currentRequest = currentRequest;
    }

    private synchronized String buildCurrentRequestHeader() {
        if (currentRequest == null) {
            return StringHelper.EMPTY_STRING;
        }

        String currentRequestHeader = SCHEMA_VERSION + SCHEMA_PIPE_DELIMITER +
                currentRequest.publicApi().getApiId() +
                SCHEMA_COMMA_DELIMITER +
                currentRequest.forceRefresh() +
                SCHEMA_PIPE_DELIMITER;

        if (currentRequestHeader.getBytes(StandardCharsets.UTF_8).length > 100) {
            log.warn("Current request telemetry header greater than 100 bytes");
        }

        return currentRequestHeader;

    }

    private synchronized String buildLastRequestHeader() {

        // LastRequest header schema:
        // schema_version|silent_successful_count|api_id1,correlation_id1|error1|
        StringBuilder lastRequestBuilder = new StringBuilder();

        lastRequestBuilder
                .append(SCHEMA_VERSION)
                .append(SCHEMA_PIPE_DELIMITER)
                .append(silentSuccessfulCount.getAndSet(0));

        // According to spec, lastRequest headers should be smaller than 350 bytes
        int baseLength = lastRequestBuilder.toString().getBytes(StandardCharsets.UTF_8).length;

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

        // In the case that lastRequestLength > 350, we should still send a string with right delimiters
        String lastRequest = lastRequestBuilder.toString() + SCHEMA_PIPE_DELIMITER + SCHEMA_PIPE_DELIMITER;
        while (it.hasNext()) {

            String correlationId = it.next();
            String[] previousRequest = previousRequests.get(correlationId);
            String apiId = (String) Array.get(previousRequest, 0);
            String error = (String) Array.get(previousRequest, 1);

            middleSegmentBuilder.append(apiId).append(SCHEMA_COMMA_DELIMITER).append(correlationId);
            errorSegmentBuilder.append(error);

            int lastRequestLength = baseLength +
                    middleSegmentBuilder.toString().getBytes(StandardCharsets.UTF_8).length +
                    errorSegmentBuilder.toString().getBytes(StandardCharsets.UTF_8).length;

            if (lastRequestLength < 348) {
                lastRequest = lastRequestBuilder.toString() +
                        middleSegmentBuilder.toString() +
                        errorSegmentBuilder.toString();
                previousRequestInProgress.put(correlationId, previousRequest);
                it.remove();
            } else {
                break;
            }

            if (it.hasNext()) {
                middleSegmentBuilder.append(SCHEMA_COMMA_DELIMITER);
                errorSegmentBuilder.append(SCHEMA_COMMA_DELIMITER);
            }

        }

        return lastRequest + SCHEMA_PIPE_DELIMITER;

    }
}
