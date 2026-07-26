// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Correlation-id link of the HTTP request chain: verifies that the correlation id sent with the
 * request matches the one returned in the response, once retries have been exhausted.
 */
class CorrelationIdRequestChain implements IRequestChain {
    private static final Logger LOG = LoggerFactory.getLogger(CorrelationIdRequestChain.class);

    private final IRequestChain next;

    CorrelationIdRequestChain(IRequestChain next) {
        this.next = next;
    }

    @Override
    public IHttpResponse executeHttpRequest(HttpRequest httpRequest, RequestContext requestContext, ServiceBundle serviceBundle)
            throws Exception {
        IHttpResponse httpResponse = next.executeHttpRequest(httpRequest, requestContext, serviceBundle);

        if (httpResponse.headers() != null) {
            verifyReturnedCorrelationId(httpRequest, httpResponse);
        }

        return httpResponse;
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
}
