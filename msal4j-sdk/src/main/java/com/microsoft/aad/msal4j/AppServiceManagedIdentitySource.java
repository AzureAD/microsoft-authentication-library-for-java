// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class AppServiceManagedIdentitySource extends AbstractManagedIdentitySource{

    private static final Logger LOG = LoggerFactory.getLogger(AppServiceManagedIdentitySource.class);

    // MSI Constants. Docs for MSI are available here https://docs.microsoft.com/azure/app-service/overview-managed-identity
    private static final String APP_SERVICE_MSI_API_VERSION = "2019-08-01";
    private static final String SECRET_HEADER_NAME = "X-IDENTITY-HEADER";
    private static URI endpointUri;

    private URI endpoint;
    private String secret;

    @Override
    public void createManagedIdentityRequest(String resource) {
        managedIdentityRequest.baseEndpoint = endpoint;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put(SECRET_HEADER_NAME, secret);

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("api-version", Collections.singletonList(APP_SERVICE_MSI_API_VERSION));
        managedIdentityRequest.queryParameters.put("resource", Collections.singletonList(resource));

        String clientId = getManagedIdentityUserAssignedClientId();
        String resourceId = getManagedIdentityUserAssignedResourceId();
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

    private AppServiceManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, URI endpoint, String secret)
    {
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.AppService);
        this.endpoint = endpoint;
        this.secret = secret;
    }

    protected static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String msiSecret = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_HEADER);
        String msiEndpoint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT);

        return validateEnvironmentVariables(msiEndpoint, msiSecret)
                ? new AppServiceManagedIdentitySource(msalRequest, serviceBundle, endpointUri, msiSecret)
                : null;
    }

    private static boolean validateEnvironmentVariables(String msiEndpoint, String secret)
    {
        endpointUri = null;

        // if BOTH the env vars endpoint and secret values are null, this MSI provider is unavailable.
        if (StringHelper.isNullOrBlank(msiEndpoint) || StringHelper.isNullOrBlank(secret))
        {
            LOG.info("[Managed Identity] App service managed identity is unavailable.");
            return false;
        }

        try
        {
            endpointUri = new URI(msiEndpoint);
        }
        catch (URISyntaxException ex)
        {
            throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT, String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "IDENTITY_ENDPOINT", msiEndpoint, "App Service"),
                    ManagedIdentitySourceType.AppService);
        }

        LOG.info("[Managed Identity] Environment variables validation passed for app service managed identity. Endpoint URI: {endpointUri}. Creating App Service managed identity.");
        return true;
    }

}
