package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class AzureArcManagedIdentity extends AbstractManagedIdentity{

    private static final String ArcApiVersion = "2019-11-01";
    private static final String AzureArc = "Azure Arc";

    private static URI endpoint;
    private final static Logger LOG = LoggerFactory.getLogger(AzureArcManagedIdentity.class);
    public AzureArcManagedIdentity(RequestContext requestContext) {

        super(requestContext, ManagedIdentitySourceType.AzureArc);
    }

    public static AbstractManagedIdentity tryCreate(RequestContext requestContext)
    {
        String identityEndpoint = EnvironmentVariables.IDENTITY_ENDPOINT;
        String imdsEndpoint = EnvironmentVariables.IMDS_ENDPOINT;

        // if BOTH the env vars IDENTITY_ENDPOINT and IMDS_ENDPOINT are set the MsiType is Azure Arc
        if (StringHelper.isNullOrBlank(identityEndpoint) || StringHelper.isNullOrBlank(imdsEndpoint))
        {
            LOG.info("[Managed Identity] Azure Arc managed identity is unavailable.");
            return null;
        }

        try {
            endpoint = new URI(identityEndpoint);
        } catch (URISyntaxException e) {
            throw new MsalManagedIdentityException(MsalError.InvalidManagedIdentityEndpoint, String.format(
                    MsalErrorMessage.MANAGED_IDENTITY_ENPOINT_INVALID_URI_ERROR, "IDENTITY_ENDPOINT", identityEndpoint, AzureArc),
                    ManagedIdentitySourceType.AzureArc);
        }

        LOG.info("[Managed Identity] Creating Azure Arc managed identity. Endpoint URI: " + endpoint);
        return new AzureArcManagedIdentity(endpoint, requestContext);
    }

    private AzureArcManagedIdentity(URI endpoint, RequestContext requestContext){
        super(requestContext, ManagedIdentitySourceType.AzureArc);
        this.endpoint = endpoint;

        if (isUserAssignedManagedIdentity())
        {
            throw new MsalManagedIdentityException(MsalError.UserAssignedManagedIdentityNotSupported,
                    String.format(MsalErrorMessage.MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED, AzureArc),
                    ManagedIdentitySourceType.AzureArc);
        }
    }

    @Override
    public ManagedIdentityRequest createRequest(String resource)
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
                throw new MsalManagedIdentityException(MsalError.ManagedIdentityRequestFailed,
                        MsalErrorMessage.MANAGED_IDENTITY_NO_CHALLENGE_ERROR,
                        ManagedIdentitySourceType.AzureArc);
            }

            String challenge = response.getHeaderValue("WWW-Authenticate");
            String[] splitChallenge = challenge.split("=");

            if (splitChallenge.length != 2)
            {
                LOG.error("[Managed Identity] The WWW-Authenticate header for Azure arc managed identity is not an expected format.");
                throw new MsalManagedIdentityException(MsalError.ManagedIdentityRequestFailed,
                        MsalErrorMessage.MANAGED_IDENTITY_INVALID_CHALLENGE,
                        ManagedIdentitySourceType.AzureArc);
            }

            String authHeaderValue = "Basic " + splitChallenge[1];

            ManagedIdentityRequest request = createRequest(parameters.getResource());

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
