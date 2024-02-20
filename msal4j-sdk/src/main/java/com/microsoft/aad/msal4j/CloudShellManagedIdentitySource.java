// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;

class CloudShellManagedIdentitySource extends AbstractManagedIdentitySource{

    private static final Logger LOG = LoggerFactory.getLogger(CloudShellManagedIdentitySource.class);

    private final URI msiEndpoint;

    @Override
    public void createManagedIdentityRequest(String resource) {
        managedIdentityRequest.baseEndpoint = msiEndpoint;
        managedIdentityRequest.method = HttpMethod.POST;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put("ContentType", "application/x-www-form-urlencoded");
        managedIdentityRequest.headers.put("Metadata", "true");

        managedIdentityRequest.bodyParameters = new HashMap<>();
        managedIdentityRequest.bodyParameters.put("resource", Collections.singletonList(resource));
    }

    private CloudShellManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, URI msiEndpoint)
    {
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.CLOUD_SHELL);
        this.msiEndpoint = msiEndpoint;

        ManagedIdentityIdType idType =
                ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getIdType();
        if (idType != ManagedIdentityIdType.SYSTEM_ASSIGNED) {
            throw new MsalServiceException(String.format(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED, "cloud shell"), MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED,
                    ManagedIdentitySourceType.CLOUD_SHELL);
        }
    }

    static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String msiEndpoint = environmentVariables.getEnvironmentVariable(Constants.MSI_ENDPOINT);


        // if ONLY the env var MSI_ENDPOINT is set the MsiType is CloudShell
        if (StringHelper.isNullOrBlank(msiEndpoint))
        {
            LOG.info("[Managed Identity] Cloud shell managed identity is unavailable.");
            return null;
        }

        return new CloudShellManagedIdentitySource(msalRequest, serviceBundle, validateAndGetUri(msiEndpoint));
    }

    private static URI validateAndGetUri(String msiEndpoint)
    {
        try
        {
            URI endpointUri = new URI(msiEndpoint);
            LOG.info("[Managed Identity] Environment variables validation passed for cloud shell managed identity. Endpoint URI: " + endpointUri + ". Creating cloud shell managed identity.");
            return endpointUri;
        }
        catch (URISyntaxException ex)
        {
            throw new MsalServiceException(String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "MSI_ENDPOINT", msiEndpoint, "Cloud Shell"), MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT,
                    ManagedIdentitySourceType.CLOUD_SHELL);
        }
    }

}
