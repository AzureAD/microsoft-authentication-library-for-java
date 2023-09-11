// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

class ManagedIdentityRequest extends MsalRequest {

    URI baseEndpoint;

    HttpMethod method;

    Map<String, String> headers;

    Map<String, String> bodyParameters;

    Map<String, String> queryParameters;

    public ManagedIdentityRequest(ManagedIdentityApplication managedIdentityApplication, RequestContext requestContext){
        super(managedIdentityApplication, requestContext);
    }

    public URL computeURI() throws URISyntaxException {
        String endpoint = this.appendQueryParametersToBaseEndpoint();
        try {
            return new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private String appendQueryParametersToBaseEndpoint(){
        StringBuilder stringBuilder = new StringBuilder(baseEndpoint.toString());
        if(!queryParameters.isEmpty()){
            stringBuilder.append("?");
        }
        boolean isFirstValue = true;
        for(String key: queryParameters.keySet()){
            if(!isFirstValue){
                stringBuilder.append("&");
            }
            String toAppend = key + "=" + queryParameters.get(key);
            stringBuilder.append(toAppend);

            isFirstValue = false;
        }

        return stringBuilder.toString();
    }
}
