// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.URIUtils;
import com.nimbusds.oauth2.sdk.util.URLUtils;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ManagedIdentityRequest extends MsalRequest {

    URI baseEndpoint;

    HttpMethod method;

    Map<String, String> headers;

    Map<String, List<String>> bodyParameters;

    Map<String, List<String>> queryParameters;

    public ManagedIdentityRequest(ManagedIdentityApplication managedIdentityApplication, RequestContext requestContext) {
        super(managedIdentityApplication, requestContext);
    }

    public String getBodyAsString() {
        if (bodyParameters == null || bodyParameters.isEmpty())
            return "";

        return URLUtils.serializeParameters(bodyParameters);
    }

    public URL computeURI() throws URISyntaxException {
        String endpoint = this.appendQueryParametersToBaseEndpoint();
        try {
            return new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String appendQueryParametersToBaseEndpoint() {
        if (queryParameters == null || queryParameters.isEmpty()) {
            return baseEndpoint.toString();
        }

        String queryString = URLUtils.serializeParameters(queryParameters);

        return baseEndpoint.toString() + "?" + queryString;
    }
}
