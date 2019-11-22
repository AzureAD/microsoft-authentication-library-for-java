// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;
import java.util.Map;

class UserDiscoveryRequest {

    // private final static Logger log = LoggerFactory.getLogger(UserDiscoveryRequest.class);

    private final static Map<String, String> HEADERS;

    static {
        HEADERS = new HashMap<>();
        HEADERS.put("Accept", "application/json, text/javascript, */*");
    }

    static UserDiscoveryResponse execute(
            final String uri,
            final Map<String, String> clientDataHeaders,
            RequestContext requestContext,
            ServiceBundle serviceBundle) {

        HashMap<String, String> headers = new HashMap<>(HEADERS);
        headers.putAll(clientDataHeaders);

        HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, uri, headers);
        IHttpResponse response = HttpHelper.executeHttpRequest(httpRequest, requestContext, serviceBundle);

        return JsonHelper.convertJsonToObject(response.body(), UserDiscoveryResponse.class);
    }
}