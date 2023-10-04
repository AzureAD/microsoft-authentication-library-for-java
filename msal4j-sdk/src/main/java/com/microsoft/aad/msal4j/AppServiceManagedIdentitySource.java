// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;

class AppServiceManagedIdentitySource extends AbstractManagedIdentitySource{

    private static final Logger LOG = LoggerFactory.getLogger(AppServiceManagedIdentitySource.class);

    // MSI Constants. Docs for MSI are available here https://docs.microsoft.com/azure/app-service/overview-managed-identity
    private static final String APP_SERVICE_MSI_API_VERSION = "2019-08-01";
    private static final String SECRET_HEADER_NAME = "X-IDENTITY-HEADER";

    private final URI MSI_ENDPOINT;
    private final String SECRET;

    @Override
    public void createManagedIdentityRequest(String resource) {
        managedIdentityRequest.baseEndpoint = MSI_ENDPOINT;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put(SECRET_HEADER_NAME, SECRET);

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("api-version", Collections.singletonList(APP_SERVICE_MSI_API_VERSION));
        managedIdentityRequest.queryParameters.put("resource", Collections.singletonList(resource));

        if (!StringHelper.isNullOrBlank(getManagedIdentityUserAssignedClientId()))
        {
            LOG.info("[Managed Identity] Adding user assigned client id to the request.");
            managedIdentityRequest.queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID, Collections.singletonList(getManagedIdentityUserAssignedClientId()));
        }

        if (!StringHelper.isNullOrBlank(getManagedIdentityUserAssignedResourceId()))
        {
            LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
            managedIdentityRequest.queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, Collections.singletonList(getManagedIdentityUserAssignedResourceId()));
        }
    }

    private AppServiceManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, URI msiEndpoint, String secret)
    {
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.APP_SERVICE);
        this.MSI_ENDPOINT = msiEndpoint;
        this.SECRET = secret;
    }

    static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String msiSecret = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_HEADER);
        String msiEndpoint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT);

        URI validatedEndpoint = validateAndGetUri(msiEndpoint, msiSecret);
        return validatedEndpoint == null ? null
                : new AppServiceManagedIdentitySource(msalRequest, serviceBundle, validatedEndpoint, msiSecret);
    }

    private static URI validateAndGetUri(String msiEndpoint, String secret)
    {
        // if BOTH the env vars endpoint and secret values are null, this MSI provider is unavailable.
        if (StringHelper.isNullOrBlank(msiEndpoint) || StringHelper.isNullOrBlank(secret))
        {
            LOG.info("[Managed Identity] App service managed identity is unavailable.");
            return null;
        }

        URI endpointUri;
        try
        {
            endpointUri = new URI(msiEndpoint);
        }
        catch (URISyntaxException ex)
        {
            throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT, String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "IDENTITY_ENDPOINT", msiEndpoint, "App Service"),
                    ManagedIdentitySourceType.APP_SERVICE);
        }

        LOG.info("[Managed Identity] Environment variables validation passed for app service managed identity. Endpoint URI: {endpointUri}. Creating App Service managed identity.");
        return endpointUri;
    }

}
