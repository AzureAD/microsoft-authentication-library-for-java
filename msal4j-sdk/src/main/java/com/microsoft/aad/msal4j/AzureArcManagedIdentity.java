// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

class AzureArcManagedIdentity extends AbstractManagedIdentitySource{

    private static final String ArcApiVersion = "2019-11-01";
    private static final String AzureArc = "Azure Arc";

    private static URI endpoint;
    private final static Logger LOG = LoggerFactory.getLogger(AzureArcManagedIdentity.class);
    public AzureArcManagedIdentity(RequestContext requestContext, ServiceBundle serviceBundle) {

        super(requestContext, serviceBundle, ManagedIdentitySourceType.AzureArc);
    }

    static AbstractManagedIdentitySource create(RequestContext requestContext, ServiceBundle serviceBundle)
    {
        String identityEndpoint = EnvironmentVariables.getIdentityEndpoint();
        String imdsEndpoint = EnvironmentVariables.getImdsEndpoint();

        // if BOTH the env vars IDENTITY_ENDPOINT and IMDS_ENDPOINT are set the MsiType is Azure Arc
        if (StringHelper.isNullOrBlank(identityEndpoint) || StringHelper.isNullOrBlank(imdsEndpoint))
        {
            LOG.info("[Managed Identity] Azure Arc managed identity is unavailable.");
            return null;
        }

        try {
            endpoint = new URI(identityEndpoint);
        } catch (URISyntaxException e) {
            throw new MsalManagedIdentityException(MsalError.INVALID_MANAGED_IDENTITY_ENDPOINT, String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENPOINT_INVALID_URI_ERROR, "IDENTITY_ENDPOINT", identityEndpoint, AzureArc),
                    ManagedIdentitySourceType.AzureArc);
        }

        LOG.info("[Managed Identity] Creating Azure Arc managed identity. Endpoint URI: " + endpoint);
        return new AzureArcManagedIdentity(endpoint, requestContext, serviceBundle );
    }

    private AzureArcManagedIdentity(URI endpoint, RequestContext requestContext, ServiceBundle serviceBundle){
        super(requestContext, serviceBundle, ManagedIdentitySourceType.AzureArc);
        this.endpoint = endpoint;

        if (isUserAssignedManagedIdentity())
        {
            throw new MsalManagedIdentityException(MsalError.USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED,
                    String.format(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED, AzureArc),
                    ManagedIdentitySourceType.AzureArc);
        }
    }

    @Override
    public ManagedIdentityRequest createManagedIdentityRequest(String resource)
    {
        ManagedIdentityRequest request = new ManagedIdentityRequest(HttpMethod.GET, endpoint);

        Map<String, String> headers = new HashMap<>();
        headers.put("Metadata", "true");
        request.headers = headers;

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("api-version", ArcApiVersion);
        queryParameters.put("resource", resource);

        request.queryParameters = queryParameters;

        return request;
    }

    @Override
    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            HTTPResponse response)
    {
        LOG.info("[Managed Identity] Response received. Status code: {response.StatusCode}");

        if (response.getStatusCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
        {
            if(!response.getHeaderMap().containsKey("WWW-Authenticate")){
                LOG.error("[Managed Identity] WWW-Authenticate header is expected but not found.");
                throw new MsalManagedIdentityException(MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                        MsalErrorMessage.MANAGED_IDENTITY_NO_CHALLENGE_ERROR,
                        ManagedIdentitySourceType.AzureArc);
            }

            String challenge = response.getHeaderValue("WWW-Authenticate");
            String[] splitChallenge = challenge.split("=");

            if (splitChallenge.length != 2)
            {
                LOG.error("[Managed Identity] The WWW-Authenticate header for Azure arc managed identity is not an expected format.");
                throw new MsalManagedIdentityException(MsalError.MANAGED_IDENTITY_REQUEST_FAILED,
                        MsalErrorMessage.MANAGED_IDENTITY_INVALID_CHALLENGE,
                        ManagedIdentitySourceType.AzureArc);
            }

            String authHeaderValue = "Basic " + splitChallenge[1];

            ManagedIdentityRequest request = createManagedIdentityRequest(parameters.getResource());

            LOG.info("[Managed Identity] Adding authorization header to the request.");

            Map<String, String> headers = request.headers;
            headers.put("Authorization", authHeaderValue);
            request.headers = headers;

//            response = requestContext.ServiceBundle.HttpManager.SendGetAsync(request.ComputeUri(), request.Headers);

            return  super.handleResponse(parameters, response);
        }

        return super.handleResponse(parameters, response);
    }
}
