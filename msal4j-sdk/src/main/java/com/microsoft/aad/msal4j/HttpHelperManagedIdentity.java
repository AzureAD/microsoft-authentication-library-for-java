// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class HttpHelperManagedIdentity extends HttpHelper {

    HttpHelperManagedIdentity(IHttpClient httpClient) {
        super(httpClient);
    }
    static final int RETRY_NUM = 3;
    static final int RETRY_DELAY_MS = 1000;

    /**
     * For most flows, MSAL Java will attempt to retry a request if the response status code is 5xx
     * <p>
     * However, for Managed Identity scenarios retry logic must be triggered only for a specific list of status codes
     */
    @Override
    boolean isRetryable(IHttpResponse httpResponse) {

        switch (httpResponse.statusCode()) {
            case 404: //Not Found
            case 408: // Request Timeout
            case 429: // Too Many Requests
            case 500: // Internal Server Error
            case 503: // Service Unavailable
            case 504: // Gateway Timeout
                return true;
            default:
                return false;
        }
    }

    @Override
    IHttpResponse executeHttpRequestWithRetries(HttpRequest httpRequest, IHttpClient httpClient)
            throws Exception {
        IHttpResponse httpResponse = null;

        //For Managed Identity, there should be three retries with a 1 second delay
        //  Starting at i = 0 for the first attempt, it will then loop 3 times (i <= RETRY_NUM)
        for (int i = 0; i <= RETRY_NUM; i++) {
            httpResponse = httpClient.send(httpRequest);
            if (!isRetryable(httpResponse)) {
                break;
            }
            Thread.sleep(RETRY_DELAY_MS);
        }

        return httpResponse;
    }
}