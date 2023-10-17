// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;

class AzureArcManagedIdentitySource extends AbstractManagedIdentitySource{

    private final static Logger LOG = LoggerFactory.getLogger(AzureArcManagedIdentitySource.class);
    private static final String ARC_API_VERSION = "2019-11-01";
    private static final String AZURE_ARC = "Azure Arc";

    private final URI MSI_ENDPOINT;

    static AbstractManagedIdentitySource create(MsalRequest msalRequest, ServiceBundle serviceBundle)
    {
        IEnvironmentVariables environmentVariables = getEnvironmentVariables((ManagedIdentityParameters) msalRequest.requestContext().apiParameters());
        String identityEndpoint = environmentVariables.getEnvironmentVariable(Constants.IDENTITY_ENDPOINT);
        String imdsEndpoint = environmentVariables.getEnvironmentVariable(Constants.IMDS_ENDPOINT);

        URI validatedUri = validateAndGetUri(identityEndpoint, imdsEndpoint);
        return validatedUri == null ? null : new AzureArcManagedIdentitySource(validatedUri, msalRequest, serviceBundle );
    }

    private static URI validateAndGetUri(String identityEndpoint, String imdsEndpoint) {

        // if BOTH the env vars IDENTITY_ENDPOINT and IMDS_ENDPOINT are set the MsiType is Azure Arc
        if (StringHelper.isNullOrBlank(identityEndpoint) || StringHelper.isNullOrBlank(imdsEndpoint))
        {
            LOG.info("[Managed Identity] Azure Arc managed identity is unavailable.");
            return null;
        }

        URI endpointUri;
        try {
            endpointUri = new URI(identityEndpoint);
        } catch (URISyntaxException e) {
            throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT, String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR, "IDENTITY_ENDPOINT", identityEndpoint, AZURE_ARC),
                    ManagedIdentitySourceType.AzureArc);
        }

        LOG.info("[Managed Identity] Creating Azure Arc managed identity. Endpoint URI: " + endpointUri);
        return endpointUri;
    }

    private AzureArcManagedIdentitySource(URI endpoint, MsalRequest msalRequest, ServiceBundle serviceBundle){
        super(msalRequest, serviceBundle, ManagedIdentitySourceType.AzureArc);
        this.MSI_ENDPOINT = endpoint;

        ManagedIdentityIdType idType =
                ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getIdType();
        if (idType != ManagedIdentityIdType.SystemAssigned) {
            throw new MsalManagedIdentityException(MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED,
                    String.format(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED, AZURE_ARC),
                    ManagedIdentitySourceType.CloudShell);
        }
    }

    @Override
    public void createManagedIdentityRequest(String resource)
    {
        managedIdentityRequest.baseEndpoint = MSI_ENDPOINT;
        managedIdentityRequest.method = HttpMethod.GET;

        managedIdentityRequest.headers = new HashMap<>();
        managedIdentityRequest.headers.put("Metadata", "true");

        managedIdentityRequest.queryParameters = new HashMap<>();
        managedIdentityRequest.queryParameters.put("api-version", Collections.singletonList(ARC_API_VERSION));
        managedIdentityRequest.queryParameters.put("resource", Collections.singletonList(resource));
    }

    @Override
    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            IHttpResponse response)
    {
        LOG.info("[Managed Identity] Response received. Status code: {response.StatusCode}");

        if (response.statusCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            if(!response.headers().containsKey("WWW-Authenticate")){
                LOG.error("[Managed Identity] WWW-Authenticate header is expected but not found.");
                throw new MsalManagedIdentityException(MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                        MsalErrorMessage.MANAGED_IDENTITY_NO_CHALLENGE_ERROR,
                        ManagedIdentitySourceType.AzureArc);
            }

            String challenge = response.headers().get("WWW-Authenticate").get(0);
            String[] splitChallenge = challenge.split("=");

            if (splitChallenge.length != 2)
            {
                LOG.error("[Managed Identity] The WWW-Authenticate header for Azure arc managed identity is not an expected format.");
                throw new MsalManagedIdentityException(MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                        MsalErrorMessage.MANAGED_IDENTITY_INVALID_CHALLENGE,
                        ManagedIdentitySourceType.AzureArc);
            }

            String authHeaderValue = "Basic " + splitChallenge[1];

            createManagedIdentityRequest(parameters.resource);

            LOG.info("[Managed Identity] Adding authorization header to the request.");

            managedIdentityRequest.headers.put("Authorization", authHeaderValue);

            try {
                response = HttpHelper.executeHttpRequest(
                        new HttpRequest(HttpMethod.GET, managedIdentityRequest.computeURI().toString(),
                                managedIdentityRequest.headers),
                        managedIdentityRequest.requestContext(),
                        serviceBundle);
            } catch (URISyntaxException e) {
                throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT,
                        MsalErrorMessage.MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR,
                        managedIdentitySourceType);
            }

            return  super.handleResponse(parameters, response);
        }

        return super.handleResponse(parameters, response);
    }
}