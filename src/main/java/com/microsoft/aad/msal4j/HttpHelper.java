// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

class HttpHelper {

    private final static Logger log = LoggerFactory.getLogger(HttpHelper.class);
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    private static String getRequestThumbprint(RequestContext requestContext) {
        String DELIMITER = ".";

        StringBuilder sb = new StringBuilder();
        sb.append(requestContext.clientId() + DELIMITER);
        sb.append(requestContext.authority() + DELIMITER);

        IApiParameters apiParameters = requestContext.apiParameters();

        if (apiParameters instanceof SilentParameters) {
            IAccount account = ((SilentParameters) apiParameters).account();
            if (account != null) {
                sb.append(account.homeAccountId() + DELIMITER);
            }
        }

        Set<String> sortedScopes = new TreeSet<>(apiParameters.scopes());
        sb.append(String.join(" ", sortedScopes));

        return StringHelper.createSha256Hash(sb.toString());
    }

    static IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                            RequestContext requestContext,
                                            ServiceBundle serviceBundle) {
        checkForThrottling(requestContext);

        HttpEvent httpEvent = new HttpEvent(); // for tracking http telemetry
        IHttpResponse httpResponse;

        try (TelemetryHelper telemetryHelper = serviceBundle.getTelemetryManager().createTelemetryHelper(
                requestContext.telemetryRequestId(),
                requestContext.clientId(),
                httpEvent,
                false)) {

            addRequestInfoToTelemetry(httpRequest, httpEvent);

            try {
                IHttpClient httpClient = serviceBundle.getHttpClient();
                httpResponse = httpClient.send(httpRequest);
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

    private static void checkForThrottling(RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication &&
                requestContext.apiParameters() != null) {
            String requestThumbprint = getRequestThumbprint(requestContext);

            long retryInMs = ThrottlingCache.retryInMs(requestThumbprint);

            if (retryInMs > 0) {
                throw new MsalThrottlingException(retryInMs);
            }
        }
    }

    private static void processThrottlingInstructions(IHttpResponse httpResponse, RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication) {
            Long expirationTimestamp = null;

            Integer retryAfterHeaderVal = getRetryAfterHeader(httpResponse);
            if (retryAfterHeaderVal != null) {
                expirationTimestamp = System.currentTimeMillis() + retryAfterHeaderVal * 1000;
            } else if (httpResponse.statusCode() == 429 ||
                    (httpResponse.statusCode() >= 500 && httpResponse.statusCode() < 600)) {

                expirationTimestamp = System.currentTimeMillis() + ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC * 1000;
            }
            if (expirationTimestamp != null) {
                ThrottlingCache.set(getRequestThumbprint(requestContext), expirationTimestamp);
            }
        }
    }

    private static Integer getRetryAfterHeader(IHttpResponse httpResponse) {

        if (httpResponse.headers() != null) {
            TreeMap<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            headers.putAll(httpResponse.headers());

            if (headers.containsKey(RETRY_AFTER_HEADER) && headers.get(RETRY_AFTER_HEADER).size() == 1) {
                int headerValue = Integer.parseInt(headers.get(RETRY_AFTER_HEADER).get(0));
                if (headerValue > 0 && headerValue <= ThrottlingCache.MAX_THROTTLING_TIME_SEC) {
                    return headerValue;
                }
            }
        }
        return null;
    }

    private static void addRequestInfoToTelemetry(final HttpRequest httpRequest, HttpEvent httpEvent) {
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

    private static void addResponseInfoToTelemetry(IHttpResponse httpResponse, HttpEvent httpEvent) {

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
