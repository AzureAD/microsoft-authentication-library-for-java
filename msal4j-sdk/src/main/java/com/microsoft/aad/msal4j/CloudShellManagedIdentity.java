// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class CloudShellManagedIdentity extends AbstractManagedIdentitySource {
    private final static Logger LOG = LoggerFactory.getLogger(CloudShellManagedIdentity.class);
    public CloudShellManagedIdentity(RequestContext requestContext, ServiceBundle serviceBundle) {
        super(requestContext, serviceBundle, ManagedIdentitySourceType.CloudShell);
    }

    private URI endpoint;
    private static final String CloudShell = "Cloud Shell";

    public static AbstractManagedIdentitySource create(RequestContext requestContext, ServiceBundle serviceBundle)
    {
        String msiEndpoint = EnvironmentVariables.getMsiEndpoint();

        // if ONLY the env var MSI_ENDPOINT is set the MsiType is CloudShell
        if (StringHelper.isNullOrBlank(msiEndpoint))
        {
            LOG.info("[Managed Identity] Cloud shell managed identity is unavailable.");
            return null;
        }

        URI endpointUri;
        try
        {
            endpointUri = new URI(msiEndpoint);
        }
        catch (URISyntaxException ex)
        {
            LOG.error("[Managed Identity] Invalid endpoint found for the environment variable MSI_ENDPOINT: " + msiEndpoint);
            throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT, String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "MSI_ENDPOINT", msiEndpoint, CloudShell),
                    ManagedIdentitySourceType.CloudShell);
        }

        LOG.info("[Managed Identity] Creating cloud shell managed identity. Endpoint URI: " + msiEndpoint);
        return new CloudShellManagedIdentity(endpointUri, requestContext, serviceBundle);
    }

    private CloudShellManagedIdentity(URI endpoint, RequestContext requestContext, ServiceBundle serviceBundle){
        super(requestContext, serviceBundle, ManagedIdentitySourceType.CloudShell);
        this.endpoint = endpoint;

        if (isUserAssignedManagedIdentity())
        {
            throw new MsalManagedIdentityException(MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED,
                    MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED,
                    ManagedIdentitySourceType.CloudShell);
        }
    }

    @Override
    public ManagedIdentityRequest createManagedIdentityRequest(String resource)
    {
        ManagedIdentityRequest request = new ManagedIdentityRequest(HttpMethod.POST, endpoint);

        Map<String, String> headers = new HashMap<>();
        headers.put("ContentType", "application/x-www-form-urlencoded");
        headers.put("Metadata", "true");
        request.headers = headers;

        Map<String, String> bodyParameters = new HashMap<>();
        bodyParameters.put("resource", resource);

        request.bodyParameters = bodyParameters;

        return request;
    }
}
