// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

/**
 * Outermost link of the HTTP request chain: applies request throttling before delegating to the
 * rest of the chain (telemetry, correlation-id verification, retry, send), and records throttling
 * instructions from the response afterwards.
 * <p>
 * This is also the composition root that wires up the rest of the chain, so unlike the other links
 * it keeps direct references to specific chain members it needs for its bespoke entry points
 * (the {@link TelemetryManager}-based overload, the correlation-id-only overload, and retry policy
 * swapping) rather than only the generic {@link IRequestChain} successor.
 */
class ThrottlingRequestChain implements IRequestChain {
    private static final Logger LOG = LoggerFactory.getLogger(ThrottlingRequestChain.class);
    public static final String RETRY_AFTER_HEADER = "Retry-After";

    private final TelemetryRequestChain telemetryChain;
    private final CorrelationIdRequestChain correlationIdChain;
    private final RetryRequestChain retryChain;

    ThrottlingRequestChain(IHttpClient httpClient, IRetryPolicy retryPolicy) {
        this(new SendRequestChain(httpClient), retryPolicy, false);
    }

    ThrottlingRequestChain(AbstractApplicationBase application, IRetryPolicy retryPolicy) {
        this(new SendRequestChain(application.httpClient()), retryPolicy, application.isRetryDisabled());
    }

    private ThrottlingRequestChain(SendRequestChain sendChain, IRetryPolicy retryPolicy, boolean retryDisabled) {
        this.retryChain = new RetryRequestChain(sendChain, retryPolicy, retryDisabled);
        this.correlationIdChain = new CorrelationIdRequestChain(retryChain);
        this.telemetryChain = new TelemetryRequestChain(correlationIdChain);
    }

    @Override
    public IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                            RequestContext requestContext,
                                            ServiceBundle serviceBundle) {
        checkForThrottling(requestContext);

        IHttpResponse httpResponse = telemetryChain.executeHttpRequest(httpRequest, requestContext, serviceBundle);

        processThrottlingInstructions(httpResponse, requestContext);

        return httpResponse;
    }

    //Overloaded version of the more commonly used HTTP executor. It does not use ServiceBundle, allowing an HTTP call to be
    // made only with more bespoke request-level parameters rather than those from the app-level ServiceBundle
    IHttpResponse executeHttpRequest(HttpRequest httpRequest,
                                     RequestContext requestContext,
                                     TelemetryManager telemetryManager) {
        checkForThrottling(requestContext);

        IHttpResponse httpResponse = telemetryChain.executeHttpRequest(httpRequest, requestContext, telemetryManager);

        processThrottlingInstructions(httpResponse, requestContext);

        return httpResponse;
    }

    IHttpResponse executeHttpRequest(HttpRequest httpRequest) {
        IHttpResponse httpResponse;

        try {
            httpResponse = correlationIdChain.executeHttpRequest(httpRequest, null, null);
        } catch (Exception e) {
            throw new MsalClientException(e);
        }

        return httpResponse;
    }

    void setRetryPolicy(IRetryPolicy retryPolicy) {
        retryChain.setRetryPolicy(retryPolicy);
    }

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

    private void processThrottlingInstructions(IHttpResponse httpResponse, RequestContext requestContext) {
        if (requestContext.clientApplication() instanceof PublicClientApplication) {
            Long expirationTimestamp = null;

            Integer retryAfterHeaderVal = getRetryAfterHeader(httpResponse);
            if (retryAfterHeaderVal != null) {
                expirationTimestamp = System.currentTimeMillis() + retryAfterHeaderVal * 1000;
            } else if (httpResponse.statusCode() == HttpStatus.HTTP_TOO_MANY_REQUESTS ||
                    (httpResponse.statusCode() >= HttpStatus.HTTP_INTERNAL_ERROR)) {

                expirationTimestamp = System.currentTimeMillis() + ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC * 1000;
            }
            if (expirationTimestamp != null) {
                ThrottlingCache.set(getRequestThumbprint(requestContext), expirationTimestamp);
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
}
