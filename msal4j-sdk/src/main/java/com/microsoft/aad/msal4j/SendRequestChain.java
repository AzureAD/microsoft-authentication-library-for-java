// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Innermost link of the HTTP request chain: sends the request via the configured {@link IHttpClient}.
 */
class SendRequestChain implements IRequestChain {
    private final IHttpClient httpClient;

    SendRequestChain(IHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public IHttpResponse executeHttpRequest(HttpRequest httpRequest, RequestContext requestContext, ServiceBundle serviceBundle)
            throws Exception {
        return httpClient.send(httpRequest);
    }
}
