// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

class UserDiscoveryRequest {

    private final static Logger log = LoggerFactory.getLogger(UserDiscoveryRequest.class);

    private final static Map<String, String> HEADERS;

    static {
        HEADERS = new HashMap<>();
        HEADERS.put("Accept", "application/json, text/javascript, */*");
    }

    static UserDiscoveryResponse execute(
            final String uri,
            final Map<String, String> clientDataHeaders,
            RequestContext requestContext,
            ServiceBundle serviceBundle) throws Exception {

        HashMap<String, String> headers = new HashMap<>(HEADERS);
        headers.putAll(clientDataHeaders);
        String response = HttpHelper.executeHttpRequest(
                log,
                HttpMethod.GET,
                uri,
                headers,
                null,
                requestContext,
                serviceBundle);

        return JsonHelper.convertJsonToObject(response, UserDiscoveryResponse.class);
    }
}