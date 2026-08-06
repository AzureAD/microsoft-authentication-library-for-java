// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

class HttpHelper implements IHttpHelper {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHelper.class);
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    private IHttpClient httpClient;
    private IRetryPolicy retryPolicy;
    private boolean retryDisabled;

    HttpHelper(IHttpClient httpClient, IRetryPolicy retryPolicy) {
        this.httpClient = httpClient;
        this.retryPolicy = retryPolicy != null ? retryPolicy : new DefaultRetryPolicy();
    }

    HttpHelper(AbstractApplicationBase application, IRetryPolicy retryPolicy) {
        this.httpClient = application.httpClient();
        this.retryDisabled = application.isRetryDisabled();
        this.retryPolicy = retryPolicy != null ? retryPolicy : new DefaultRetryPolicy();
    }

    public IHttpResponse executeHttpRequest(HttpRequest httpRequest,
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

    //Overloaded version of the more commonly used HTTP executor. It does not use ServiceBundle, allowing an HTTP call to be
    // made only with more bespoke request-level parameters rather than those from the app-level ServiceBundle
    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                            RequestContext requestContext,
                                            TelemetryManager telemetryManager,
                                            IHttpClient httpClient) {
        checkForThrottling(requestContext);

        HttpEvent httpEvent = new HttpEvent(); // for tracking http telemetry
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

    /*
     * Two throttle fingerprints are derived from a request:
     *
     *  - The app-wide thumbprint (includeUser == false) keys on clientId + authority + scope only.
     *    It is used for service-directed rate limiting (HTTP 429 and explicit Retry-After), which
     *    applies to the whole client regardless of which user made the request.
     *
     *  - The user-aware thumbprint (includeUser == true) additionally folds in the request's user
     *    component (UPN, else OID). It is used for error-class throttling (HTTP 5xx), which can be
     *    specific to a single user (e.g. ADFS returns HTTP 500 for one user's bad password) and must
     *    not block other users of the same client.
     */
    private String getRequestThumbprint(RequestContext requestContext) {
        return getRequestThumbprint(requestContext, true);
    }

    private String getRequestThumbprint(RequestContext requestContext, boolean includeUser) {
        StringBuilder sb = new StringBuilder();
        sb.append(requestContext.clientId()).append(POINT_DELIMITER);
        sb.append(requestContext.authority()).append(POINT_DELIMITER);

        if (includeUser) {
            UserIdentifier userIdentifier = requestContext.userIdentifier();
            if (userIdentifier != null) {
                if (!StringHelper.isBlank(userIdentifier.upn())) {
                    sb.append(userIdentifier.upn()).append(POINT_DELIMITER);
                } else if (!StringHelper.isBlank(userIdentifier.oid())) {
                    sb.append(userIdentifier.oid()).append(POINT_DELIMITER);
                }
            }
        }

        IAcquireTokenParameters apiParameters = requestContext.apiParameters();
        Set<String> sortedScopes = new TreeSet<>(apiParameters.scopes());
        sb.append(String.join(" ", sortedScopes));

        return StringHelper.createSha256Hash(sb.toString());
    }

    IHttpResponse executeHttpRequestWithRetries(HttpRequest httpRequest, IHttpClient httpClient)
            throws Exception {
        IHttpResponse httpResponse = httpClient.send(httpRequest);

        if (retryDisabled) {
            return httpResponse;
        }

        int retryCount = 0;
        int maxRetries = retryPolicy.getMaxRetryCount(httpResponse);

        while (retryPolicy.isRetryable(httpResponse) && retryCount < maxRetries) {
            Thread.sleep(retryPolicy.getRetryDelayMs(httpResponse));

            retryCount++;

            httpResponse = httpClient.send(httpRequest);
        }

        return httpResponse;
    }

    private void checkForThrottling(RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication &&
                requestContext.apiParameters() != null) {
            // Check the app-wide key first (429 / Retry-After entries), then the user-aware key
            // (5xx entries) when it differs from the app-wide key.
            String appWideThumbprint = getRequestThumbprint(requestContext, false);
            long retryInMs = ThrottlingCache.retryInMs(appWideThumbprint);

            if (retryInMs <= 0) {
                String userAwareThumbprint = getRequestThumbprint(requestContext, true);
                if (!userAwareThumbprint.equals(appWideThumbprint)) {
                    retryInMs = ThrottlingCache.retryInMs(userAwareThumbprint);
                }
            }

            if (retryInMs > 0) {
                throw new MsalThrottlingException(retryInMs);
            }
        }
    }

    private void processThrottlingInstructions(IHttpResponse httpResponse, RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication) {
            Long expirationTimestamp = null;
            // 5xx errors can be user-specific, so they are throttled per-user; 429 and explicit
            // Retry-After are service-directed and are throttled app-wide.
            boolean userScoped = false;

            Integer retryAfterHeaderVal = getRetryAfterHeader(httpResponse);
            if (retryAfterHeaderVal != null) {
                expirationTimestamp = System.currentTimeMillis() + retryAfterHeaderVal * 1000;
            } else if (httpResponse.statusCode() == HttpStatus.HTTP_TOO_MANY_REQUESTS) {
                expirationTimestamp = System.currentTimeMillis() + ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC * 1000;
            } else if (httpResponse.statusCode() >= HttpStatus.HTTP_INTERNAL_ERROR) {
                expirationTimestamp = System.currentTimeMillis() + ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC * 1000;
                userScoped = true;
            }
            if (expirationTimestamp != null) {
                ThrottlingCache.set(getRequestThumbprint(requestContext, userScoped), expirationTimestamp);
            }
        }
    }

    static Integer getRetryAfterHeader(IHttpResponse httpResponse) {

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
                    LOG.warn("Failed to parse value of Retry-After header - NumberFormatException");
                }
            }
        }
        return null;
    }

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

            LOG.warn(LogHelper.createMessage("Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    correlationId != null ? correlationId : ""));
        }
    }

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

            LOG.info(msg);
        }
    }

    void setRetryPolicy(IRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
}
