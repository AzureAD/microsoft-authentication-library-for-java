// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ManagedIdentityRequest extends MsalRequest {

    private static final Logger LOG = LoggerFactory.getLogger(ManagedIdentityRequest.class);

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

    void addUserAssignedIdToQuery(ManagedIdentityIdType idType, String userAssignedId) {
        switch (idType) {
            case CLIENT_ID:
                LOG.info("[Managed Identity] Adding user assigned client id to the request.");
                queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID, Collections.singletonList(userAssignedId));
                break;
            case RESOURCE_ID:
                LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
                if (ManagedIdentityClient.getManagedIdentitySource() == ManagedIdentitySourceType.IMDS) {
                    // IMDS seems to accept both mi_res_id and msi_res_id but their API only documents msi_res_id,
                    // and using mi_res_id leads to issues in some scenarios that use the IMDS code path.
                    queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID_IMDS, Collections.singletonList(userAssignedId));
                } else {
                    queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, Collections.singletonList(userAssignedId));
                }
                break;
            case OBJECT_ID:
                LOG.info("[Managed Identity] Adding user assigned object id to the request.");
                queryParameters.put(Constants.MANAGED_IDENTITY_OBJECT_ID, Collections.singletonList(userAssignedId));
                break;
        }
    }

    void addTokenRevocationParametersToQuery(ManagedIdentityParameters parameters) {
        // Check if the environment supports token revocation
        ManagedIdentitySourceType sourceType = ManagedIdentityClient.getManagedIdentitySource();
        boolean supportsTokenRevocation = Constants.TOKEN_REVOCATION_SUPPORTED_ENVIRONMENTS
                .contains(sourceType);

        // If token revocation is supported, pass the client capabilities and token revocation parameters
        if (supportsTokenRevocation) {
            ManagedIdentityApplication managedIdentityApplication =
                    (ManagedIdentityApplication) this.application();

            // Pass capabilities if present.
            if (managedIdentityApplication.getClientCapabilities() != null &&
                    !managedIdentityApplication.getClientCapabilities().isEmpty()) {
                // Add client capabilities as a comma separated string for all the values in client capabilities
                String clientCapabilities = String.join(",", managedIdentityApplication.getClientCapabilities());

                queryParameters.put(Constants.CLIENT_CAPABILITY_REQUEST_PARAM, Collections.singletonList(clientCapabilities.toString()));
            }

            // Pass the token revocation parameter if the claims are present and there is a token to revoke
            if (!StringHelper.isNullOrBlank(parameters.claims) && !StringHelper.isNullOrBlank(parameters.revokedTokenHash())) {
                LOG.info("[Managed Identity] Adding token revocation parameter to request");
                if (queryParameters == null) {
                    queryParameters = new HashMap<>();
                }
                queryParameters.put(Constants.TOKEN_HASH_CLAIM, Collections.singletonList(parameters.revokedTokenHash()));
            }
        }
    }
}
