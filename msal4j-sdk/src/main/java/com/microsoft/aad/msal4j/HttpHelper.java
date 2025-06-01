// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.*;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

/**
 * Helper class for handling HTTP requests and responses with retry and throttling logic.
 */
class HttpHelper implements IHttpHelper {

    private static final Logger log = LoggerFactory.getLogger(HttpHelper.class);

    /**
     * Header name for specifying retry-after duration.
     */
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    /**
     * Set of exception types that are considered acceptable for retry.
     */
    private static final HashSet<Class<? extends Exception>> ACCEPTABLE_EXCEPTIONS = new HashSet<>();

    /**
     * Number of retry attempts for HTTP requests.
     */
    private static final int RETRY_NUM = 2;

    /**
     * Delay in milliseconds between retry attempts.
     */
    private static final int RETRY_DELAY_MS = 1000;

    static {
        ACCEPTABLE_EXCEPTIONS.add(ConnectException.class);
        ACCEPTABLE_EXCEPTIONS.add(SocketTimeoutException.class);
        ACCEPTABLE_EXCEPTIONS.add(IOException.class);
    }

    /**
     * RetryableCall instance for executing HTTP requests with retry logic.
     */
    private static final RetryableCall<IHttpResponse> RETRYABLE_CALL =
            new RetryableCall<>(ACCEPTABLE_EXCEPTIONS, RETRY_NUM, RETRY_DELAY_MS);

    /**
     * HTTP status code for OK.
     */
    public static final int HTTP_STATUS_200 = 200;

    /**
     * HTTP status code for Bad Request.
     */
    public static final int HTTP_STATUS_400 = 400;

    /**
     * HTTP status code for Too Many Requests.
     */
    public static final int HTTP_STATUS_429 = 429;

    /**
     * HTTP status code for Internal Server Error.
     */
    public static final int HTTP_STATUS_500 = 500;

    private IHttpClient httpClient;

    /**
     * Constructs an instance of HttpHelper with the specified HTTP client.
     *
     * @param httpClient The HTTP client to use for sending requests.
     */
    HttpHelper(IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Executes an HTTP request with retry and telemetry logic.
     *
     * @param httpRequest   The HTTP request to execute.
     * @param requestContext The context of the request, including telemetry and client information.
     * @param serviceBundle  The service bundle containing application-level configurations.
     * @return The HTTP response received from the server.
     */
    public IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                            RequestContext requestContext,
                                            ServiceBundle serviceBundle) {
        checkForThrottling(requestContext);

        HttpEvent httpEvent = new HttpEvent(); // for tracking HTTP telemetry
        IHttpResponse httpResponse;

        try (TelemetryHelper telemetryHelper = serviceBundle.getTelemetryManager().createTelemetryHelper(
                requestContext.telemetryRequestId(),
                requestContext.clientId(),
                httpEvent,
                false)) {

            addRequestInfoToTelemetry(httpRequest, httpEvent);

            try {
                httpResponse = executeHttpRequestWithRetries(httpRequest, httpClient);

            } catch (Exception e) {
                httpEvent.setOauthErrorCode(AuthenticationErrorCode.UNKNOWN);
                throw new MsalClientException(e);
            }

            addResponseInfoToTelemetry(httpResponse, httpEvent);

            if (httpResponse.headers() != null) {
                HttpHelper.verifyReturnedCorrelationId(httpRequest, httpResponse);
            }
        }
        processThrottlingInstructions(httpResponse, requestContext);

        return httpResponse;
    }

    /**
     * Overloaded version of the HTTP executor that does not use ServiceBundle.
     *
     * @param httpRequest    The HTTP request to execute.
     * @param requestContext The context of the request, including telemetry and client information.
     * @param telemetryManager The telemetry manager for tracking request telemetry.
     * @param httpClient     The HTTP client to use for sending requests.
     * @return The HTTP response received from the server.
     */
    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                     RequestContext requestContext,
                                     TelemetryManager telemetryManager,
                                     IHttpClient httpClient) {
        checkForThrottling(requestContext);

        HttpEvent httpEvent = new HttpEvent(); // for tracking HTTP telemetry
        IHttpResponse httpResponse;

        try (TelemetryHelper telemetryHelper = telemetryManager.createTelemetryHelper(
                requestContext.telemetryRequestId(),
                requestContext.clientId(),
                httpEvent,
                false)) {

            addRequestInfoToTelemetry(httpRequest, httpEvent);

            try {
                httpResponse = executeHttpRequestWithRetries(httpRequest, httpClient);

            } catch (Exception e) {
                httpEvent.setOauthErrorCode(AuthenticationErrorCode.UNKNOWN);
                throw new MsalClientException(e);
            }

            addResponseInfoToTelemetry(httpResponse, httpEvent);

            if (httpResponse.headers() != null) {
                HttpHelper.verifyReturnedCorrelationId(httpRequest, httpResponse);
            }
        }
        processThrottlingInstructions(httpResponse, requestContext);

        return httpResponse;
    }

    /**
     * Executes an HTTP request without additional context or telemetry.
     *
     * @param httpRequest The HTTP request to execute.
     * @return The HTTP response received from the server.
     */
    IHttpResponse executeHttpRequest(HttpRequest httpRequest) {
        IHttpResponse httpResponse;

        try {
            httpResponse = executeHttpRequestWithRetries(httpRequest, httpClient);
        } catch (Exception e) {
            throw new MsalClientException(e);
        }

        if (httpResponse.headers() != null) {
            HttpHelper.verifyReturnedCorrelationId(httpRequest, httpResponse);
        }

        return httpResponse;
    }

    /**
     * Generates a unique request thumbprint for throttling purposes.
     *
     * @param requestContext The context of the request.
     * @return A SHA-256 hash representing the request thumbprint.
     */
    private String getRequestThumbprint(RequestContext requestContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(requestContext.clientId() + POINT_DELIMITER);
        sb.append(requestContext.authority() + POINT_DELIMITER);

        IAcquireTokenParameters apiParameters = requestContext.apiParameters();

        if (apiParameters instanceof SilentParameters) {
            IAccount account = ((SilentParameters) apiParameters).account();
            if (account != null) {
                sb.append(account.homeAccountId() + POINT_DELIMITER);
            }
        }

        Set<String> sortedScopes = new TreeSet<>(apiParameters.scopes());
        sb.append(String.join(" ", sortedScopes));

        return StringHelper.createSha256Hash(sb.toString());
    }

    /**
     * Determines if the HTTP response is retryable based on its status code.
     *
     * @param httpResponse The HTTP response to evaluate.
     * @return True if the response is retryable, false otherwise.
     */
    boolean isRetryable(IHttpResponse httpResponse) {
        return httpResponse.statusCode() >= HTTP_STATUS_500 &&
                getRetryAfterHeader(httpResponse) == null;
    }

    /**
     * Executes an HTTP request with retry logic.
     *
     * @param httpRequest The HTTP request to execute.
     * @param httpClient  The HTTP client to use for sending requests.
     * @return The HTTP response received from the server.
     * @throws Exception If the request fails after all retry attempts.
     */
    IHttpResponse executeHttpRequestWithRetries(HttpRequest httpRequest, IHttpClient httpClient)
            throws Exception {
        IHttpResponse httpResponse = null;
        for (int i = 0; i < RETRY_NUM; i++) {
            httpResponse = RETRYABLE_CALL.callWithRetry(() -> httpClient.send(httpRequest));
            if (!isRetryable(httpResponse)) {
                break;
            }
            Thread.sleep(RETRY_DELAY_MS);
        }

        return httpResponse;
    }

    /**
     * Checks if the request is throttled and throws an exception if necessary.
     *
     * @param requestContext The context of the request.
     */
    private void checkForThrottling(RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication &&
                requestContext.apiParameters() != null) {
            String requestThumbprint = getRequestThumbprint(requestContext);

            long retryInMs = ThrottlingCache.retryInMs(requestThumbprint);

            if (retryInMs > 0) {
                throw new MsalThrottlingException(retryInMs);
            }
        }
    }

    /**
     * Processes throttling instructions based on the HTTP response.
     *
     * @param httpResponse   The HTTP response received.
     * @param requestContext The context of the request.
     */
    private void processThrottlingInstructions(IHttpResponse httpResponse, RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication) {
            Long expirationTimestamp = null;

            Integer retryAfterHeaderVal = getRetryAfterHeader(httpResponse);
            if (retryAfterHeaderVal != null) {
                expirationTimestamp = System.currentTimeMillis() + retryAfterHeaderVal * 1000;
            } else if (httpResponse.statusCode() == HTTP_STATUS_429 ||
                    (httpResponse.statusCode() >= HTTP_STATUS_500)) {

                expirationTimestamp = System.currentTimeMillis() + ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC * 1000;
            }
            if (expirationTimestamp != null) {
                ThrottlingCache.set(getRequestThumbprint(requestContext), expirationTimestamp);
            }
        }
    }

    /**
     * Retrieves the Retry-After header value from the HTTP response.
     *
     * @param httpResponse The HTTP response to evaluate.
     * @return The Retry-After value in seconds, or null if not present or invalid.
     */
    private Integer getRetryAfterHeader(IHttpResponse httpResponse) {

        if (httpResponse.headers() != null) {
            TreeMap<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll(httpResponse.headers());

            if (headers.containsKey(RETRY_AFTER_HEADER) && headers.get(RETRY_AFTER_HEADER).size() == 1) {
                try {
                    int headerValue = Integer.parseInt(headers.get(RETRY_AFTER_HEADER).get(0));

                    if (headerValue > 0 && headerValue <= ThrottlingCache.MAX_THROTTLING_TIME_SEC) {
                        return headerValue;
                    }
                } catch (NumberFormatException ex) {
                    log.warn("Failed to parse value of Retry-After header - NumberFormatException");
                }
            }
        }
        return null;
    }

    /**
     * Adds request information to the telemetry event.
     *
     * @param httpRequest The HTTP request being executed.
     * @param httpEvent   The telemetry event to update.
     */
    private void addRequestInfoToTelemetry(final HttpRequest httpRequest, HttpEvent httpEvent) {
        try {
            httpEvent.setHttpPath(httpRequest.url().toURI());
            httpEvent.setHttpMethod(httpRequest.httpMethod().toString());
            if (!StringHelper.isBlank(httpRequest.url().getQuery())) {
                httpEvent.setQueryParameters(httpRequest.url().getQuery());
            }
        } catch (Exception ex) {
            String correlationId = httpRequest.headerValue(
                    HttpHeaders.CORRELATION_ID_HEADER_NAME);

            log.warn(LogHelper.createMessage("Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    correlationId != null ? correlationId : ""));
        }
    }

    /**
     * Adds response information to the telemetry event.
     *
     * @param httpResponse The HTTP response received.
     * @param httpEvent    The telemetry event to update.
     */
    private void addResponseInfoToTelemetry(IHttpResponse httpResponse, HttpEvent httpEvent) {

        httpEvent.setHttpResponseStatus(httpResponse.statusCode());

        Map<String, List<String>> headers = httpResponse.headers();

        String userAgent = HttpUtils.headerValue(headers, "User-Agent");
        if (!StringHelper.isBlank(userAgent)) {
            httpEvent.setUserAgent(userAgent);
        }

        String xMsRequestId = HttpUtils.headerValue(headers, "x-ms-request-id");
        if (!StringHelper.isBlank(xMsRequestId)) {
            httpEvent.setRequestIdHeader(xMsRequestId);
        }

        String xMsClientTelemetry = HttpUtils.headerValue(headers, "x-ms-clitelem");
        if (xMsClientTelemetry != null) {
            XmsClientTelemetryInfo xmsClientTelemetryInfo =
                    XmsClientTelemetryInfo.parseXmsTelemetryInfo(xMsClientTelemetry);

            if (xmsClientTelemetryInfo != null) {
                httpEvent.setXmsClientTelemetryInfo(xmsClientTelemetryInfo);
            }
        }
    }

    /**
     * Verifies that the correlation ID returned in the HTTP response matches the one sent in the request.
     *
     * @param httpRequest  The HTTP request sent.
     * @param httpResponse The HTTP response received.
     */
    private static void verifyReturnedCorrelationId(final HttpRequest httpRequest,
                                                    IHttpResponse httpResponse) {

        String sentCorrelationId = httpRequest.headerValue(
                HttpHeaders.CORRELATION_ID_HEADER_NAME);

        String returnedCorrelationId = HttpUtils.headerValue(
                httpResponse.headers(),
                HttpHeaders.CORRELATION_ID_HEADER_NAME);

        if (StringHelper.isBlank(returnedCorrelationId) ||
                !returnedCorrelationId.equals(sentCorrelationId)) {

            String msg = LogHelper.createMessage(
                    String.format(
                            "Sent (%s) Correlation Id is not same as received (%s).",
                            sentCorrelationId,
                            returnedCorrelationId),
                    sentCorrelationId);

            log.info(msg);
        }
    }
}