// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;

class ServiceFabricManagedIdentitySource extends AbstractManagedIdentitySource {

    private static final Logger LOG = LoggerFactory.getLogger(ServiceFabricManagedIdentitySource.class);

    private static final String SERVICE_FABRIC_MSI_API_VERSION = "2019-07-01-preview";

    private final URI msiEndpoint;
    private final String identityHeader;
    private final ManagedIdentityIdType idType;
    private final String userAssignedId;

    @Override
    public void createManagedIdentityRequest(String resource) {
        managedIdentityRequest.baseEndpoint = msiEndpoint;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put("secret", identityHeader);

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("resource", Collections.singletonList(resource));
        managedIdentityRequest.queryParameters.put("api-version", Collections.singletonList(SERVICE_FABRIC_MSI_API_VERSION));

        if (idType == ManagedIdentityIdType.CLIENT_ID) {
            LOG.info("[Managed Identity] Adding user assigned client id to the request for Service Fabric Managed Identity.");
            managedIdentityRequest.queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID, Collections.singletonList(userAssignedId));
        } else if (idType == ManagedIdentityIdType.RESOURCE_ID) {
            LOG.info("[Managed Identity] Adding user assigned resource id to the request for Service Fabric Managed Identity.");
            managedIdentityRequest.queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, Collections.singletonList(userAssignedId));
        }
    }

    private ServiceFabricManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, URI msiEndpoint, String identityHeader)
    {
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.SERVICE_FABRIC);
        this.msiEndpoint = msiEndpoint;
        this.identityHeader = identityHeader;

        this.idType = ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getIdType();
        this.userAssignedId = ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getUserAssignedId();
    }

    static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String msiEndpoint = environmentVariables.getEnvironmentVariable(Constants.MSI_ENDPOINT);
        String identityHeader = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT);
        String identityServerThumbprint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_SERVER_THUMBPRINT);


        if (StringHelper.isNullOrBlank(msiEndpoint) || StringHelper.isNullOrBlank(identityHeader) || StringHelper.isNullOrBlank(identityServerThumbprint))
        {
            LOG.info("[Managed Identity] Service fabric managed identity is unavailable.");
            return null;
        }

        return new ServiceFabricManagedIdentitySource(msalRequest, serviceBundle, validateAndGetUri(msiEndpoint), identityHeader);
    }

    private static URI validateAndGetUri(String msiEndpoint)
    {
        try
        {
            URI endpointUri = new URI(msiEndpoint);
            LOG.info("[Managed Identity] Environment variables validation passed for Service Fabric Managed Identity. Endpoint URI: " + endpointUri);
            return endpointUri;
        }
        catch (URISyntaxException ex)
        {
            throw new MsalServiceException(String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "MSI_ENDPOINT", msiEndpoint, "Service Fabric"), MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT,
                    ManagedIdentitySourceType.SERVICE_FABRIC);
        }
    }

}