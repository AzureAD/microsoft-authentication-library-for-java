// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

class AppServiceManagedIdentity extends AbstractManagedIdentitySource{

    private final static Logger LOG = LoggerFactory.getLogger(AppServiceManagedIdentity.class);

    // MSI Constants. Docs for MSI are available here https://docs.microsoft.com/azure/app-service/overview-managed-identity
    private static final String APP_SERVICE_MSI_API_VERSION = "2019-08-01";
    private static final String SecretHeaderName = "X-IDENTITY-HEADER";

    private URI endpoint;
    private String secret;

    private static URI endpointUri;

    @Override
    public void createManagedIdentityRequest(String resource) {
        Map<String, String> headers = new HashMap<>();
        headers.put(SecretHeaderName, secret);
        managedIdentityRequest.headers = headers;

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("api-version", APP_SERVICE_MSI_API_VERSION );
        queryParameters.put("resource", resource);

        if (!StringHelper.isNullOrBlank(getManagedIdentityUserAssignedClientId()))
        {
            LOG.info("[Managed Identity] Adding user assigned client id to the request.");
            queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID, getManagedIdentityUserAssignedClientId());
        }

        if (!StringHelper.isNullOrBlank(getManagedIdentityUserAssignedResourceId()))
        {
            LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
            queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, getManagedIdentityUserAssignedResourceId());
        }

        managedIdentityRequest.queryParameters = queryParameters;
    }

    private AppServiceManagedIdentity(MsalRequest msalRequest, ServiceBundle serviceBundle, URI endpoint, String secret)
    {
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.AppService);
        this.endpoint = endpoint;
        this.secret = secret;
    }

    protected static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String msiSecret = environmentVariables.getEnvironmentVariable(IEnvironmentVariables.IDENTITY_HEADER);
        String msiEndpoint = environmentVariables.getEnvironmentVariable(IEnvironmentVariables.IDENTITY_ENDPOINT);

        return validateEnvironmentVariables(msiEndpoint, msiSecret)
                ? new AppServiceManagedIdentity(msalRequest, serviceBundle, endpointUri, msiSecret)
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
