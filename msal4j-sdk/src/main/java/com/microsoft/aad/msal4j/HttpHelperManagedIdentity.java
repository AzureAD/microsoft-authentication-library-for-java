// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class HttpHelperManagedIdentity extends HttpHelper {

    HttpHelperManagedIdentity(IHttpClient httpClient) {
        super(httpClient);
    }

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
}