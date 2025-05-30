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

    //Service Fabric requires a special check for an environment variable containing a certificate thumbprint used for validating requests.
    //No other flow need this and an app developer may not be aware of it, so it was decided that for the Service Fabric flow we will simply override
    // any HttpClient that may have been set by the app developer with our own client which performs the validation logic.
    private static IHttpClient httpClient = new DefaultHttpClientManagedIdentity(null, null, null, null);
    private static HttpHelper httpHelper = new HttpHelper(httpClient, new ManagedIdentityRetryPolicy());

    @Override
    public void createManagedIdentityRequest(String resource) {
        managedIdentityRequest.baseEndpoint = msiEndpoint;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put("secret", identityHeader);

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("resource", Collections.singletonList(resource));
        managedIdentityRequest.queryParameters.put("api-version", Collections.singletonList(SERVICE_FABRIC_MSI_API_VERSION));

        if (this.idType != null && !StringHelper.isNullOrBlank(this.userAssignedId)) {
            LOG.info("[Managed Identity] Adding user assigned ID to the request for Service Fabric Managed Identity.");
            managedIdentityRequest.addUserAssignedIdToQuery(this.idType, this.userAssignedId);
        }
    }

    private ServiceFabricManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle, URI msiEndpoint, String identityHeader)
    {
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.SERVICE_FABRIC);
        this.msiEndpoint = msiEndpoint;
        this.identityHeader = identityHeader;
    }

    @Override
    public ManagedIdentityResponse getManagedIdentityResponse(
            ManagedIdentityParameters parameters) {

        createManagedIdentityRequest(parameters.resource);
        managedIdentityRequest.addTokenRevocationParametersToQuery(parameters);
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
                    httpClient);
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

        IEnvironmentVariables environmentVariables = getEnvironmentVariables();
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

    //The HttpClient is not normally customizable in this flow, as it requires special behavior for certificate validation.
    //However, unit tests often need to mock HttpClient and need a way to inject the mocked object into this class.
    static void setHttpClient(IHttpClient client) {
        httpClient = client;
        httpHelper = new HttpHelper(httpClient, new ManagedIdentityRetryPolicy());
    }
}