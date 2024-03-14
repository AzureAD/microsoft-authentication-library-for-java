// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketException;
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
    private final IHttpClient httpClient = new DefaultHttpClientManagedIdentity(null, null, null, null);
    private final HttpHelper httpHelper = new HttpHelper(httpClient);

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

    @Override
    public ManagedIdentityResponse getManagedIdentityResponse(
            ManagedIdentityParameters parameters) {

        createManagedIdentityRequest(parameters.resource);
        IHttpResponse response;

        try {

            HttpRequest httpRequest = managedIdentityRequest.method.equals(HttpMethod.GET) ?
                    new HttpRequest(HttpMethod.GET,
                            managedIdentityRequest.computeURI().toString(),
                            managedIdentityRequest.headers) :
                    new HttpRequest(HttpMethod.POST,
                            managedIdentityRequest.computeURI().toString(),
                            managedIdentityRequest.headers,
                            managedIdentityRequest.getBodyAsString());

            response = httpHelper.executeHttpRequest(httpRequest, managedIdentityRequest.requestContext(), serviceBundle.getTelemetryManager(),
                    new DefaultHttpClientManagedIdentity(null, null, null, null));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (MsalClientException e) {
            if (e.getCause() instanceof SocketException) {
                throw new MsalServiceException(e.getMessage(), MsalError.MANAGED_IDENTITY_UNREACHABLE_NETWORK, managedIdentitySourceType);
            }

            throw e;
        }

        return handleResponse(parameters, response);
    }

    static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle) {

        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String identityEndpoint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT);
        String identityHeader = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_HEADER);
        String identityServerThumbprint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_SERVER_THUMBPRINT);


        if (StringHelper.isNullOrBlank(identityEndpoint) || StringHelper.isNullOrBlank(identityHeader) || StringHelper.isNullOrBlank(identityServerThumbprint))
        {
            LOG.info("[Managed Identity] Service fabric managed identity is unavailable.");
            return null;
        }

        return new ServiceFabricManagedIdentitySource(msalRequest, serviceBundle, validateAndGetUri(identityEndpoint), identityHeader);
    }

    private static URI validateAndGetUri(String msiEndpoint)
    {
        try
        {
            URI endpointUri = new URI(msiEndpoint);
            LOG.info(String.format("[Managed Identity] Environment variables validation passed for Service Fabric Managed Identity. Endpoint URI: %s", endpointUri));
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