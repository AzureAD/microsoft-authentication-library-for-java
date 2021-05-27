// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

class UserDiscoveryRequest {

    private static final Map<String, String> HEADERS;

    static {
        HEADERS = new HashMap<>();
        HEADERS.put("Accept", "application/json, text/javascript, */*");
    }

    private UserDiscoveryRequest() {
    }

    static UserDiscoveryResponse execute(
            final String uri,
            Map<String, String> clientDataHeaders,
            RequestContext requestContext,
            ServiceBundle serviceBundle) {

        HashMap<String, String> headers = new HashMap<>(HEADERS);
        headers.putAll(clientDataHeaders);

        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, uri, headers);
        IHttpResponse response = HttpHelper.executeHttpRequest(httpRequest, requestContext, serviceBundle);

        if (response.statusCode() != HttpHelper.HTTP_STATUS_200) {
            throw MsalServiceExceptionFactory.fromHttpResponse(response);
        }
        return JsonHelper.convertJsonToObject(response.body(), UserDiscoveryResponse.class);
    }
}