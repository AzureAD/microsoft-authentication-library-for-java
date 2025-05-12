// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

class ManagedIdentityRequest extends MsalRequest {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityRequest.class);

    URI baseEndpoint;

    HttpMethod method;

    Map<String, String> headers;

    Map<String, String> bodyParameters;

    Map<String, String> queryParameters;

    public ManagedIdentityRequest(ManagedIdentityApplication managedIdentityApplication, RequestContext requestContext) {
        super(managedIdentityApplication, requestContext);
    }

    public String getBodyAsString() {
        if (bodyParameters == null || bodyParameters.isEmpty())
            return "";

        return StringHelper.serializeQueryParameters(bodyParameters);
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

        String queryString = StringHelper.serializeQueryParameters(queryParameters);

        return baseEndpoint.toString() + "?" + queryString;
    }

    void addUserAssignedIdToQuery(ManagedIdentityIdType idType, String userAssignedId) {
        switch (idType) {
            case CLIENT_ID:
                LOG.info("[Managed Identity] Adding user assigned client id to the request.");
                queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID, userAssignedId);
                break;
            case RESOURCE_ID:
                LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
                queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, userAssignedId);
                break;
            case OBJECT_ID:
                LOG.info("[Managed Identity] Adding user assigned object id to the request.");
                queryParameters.put(Constants.MANAGED_IDENTITY_OBJECT_ID, userAssignedId);
                break;
        }
    }
}
