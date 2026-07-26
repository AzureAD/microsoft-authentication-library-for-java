// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Retry link of the HTTP request chain: retries the request, based on either the HTTP response
 * or an exception thrown while sending, according to the configured retry policies.
 */
class RetryRequestChain implements IRequestChain {
    private final IRequestChain next;
    private IRetryPolicy retryPolicy;
    private IRetryableExceptionPolicy retryableExceptionPolicy = new DefaultRetryableExceptionPolicy();
    private boolean retryDisabled;

    RetryRequestChain(IRequestChain next, IRetryPolicy retryPolicy, boolean retryDisabled) {
        this.next = next;
        this.retryPolicy = retryPolicy != null ? retryPolicy : new DefaultRetryPolicy();
        this.retryDisabled = retryDisabled;
    }

    @Override
    public IHttpResponse executeHttpRequest(HttpRequest httpRequest, RequestContext requestContext, ServiceBundle serviceBundle)
            throws Exception {
        IHttpResponse httpResponse = null;
        Exception caughtException = null;
        int retryCount = 0;
        boolean retry;

        do {
            try {
                httpResponse = next.executeHttpRequest(httpRequest, requestContext, serviceBundle);
                caughtException = null;
            } catch (Exception e) {
                httpResponse = null;
                caughtException = e;
            }

            retry = false;

            if (!retryDisabled) {
                if (caughtException != null) {
                    if (retryCount < retryableExceptionPolicy.getMaxRetryCount(caughtException)
                            && retryableExceptionPolicy.isRetryable(caughtException)) {
                        Thread.sleep(retryableExceptionPolicy.getRetryDelayMs(caughtException));
                        retryCount++;
                        retry = true;
                    }
                } else {
                    if (retryCount < retryPolicy.getMaxRetryCount(httpResponse)
                            && retryPolicy.isRetryable(httpResponse)) {
                        Thread.sleep(retryPolicy.getRetryDelayMs(httpResponse));
                        retryCount++;
                        retry = true;
                    }
                }
            }
        } while (retry);

        if (caughtException != null) {
            throw caughtException;
        }

        return httpResponse;
    }

    void setRetryPolicy(IRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }
}
