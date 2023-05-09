package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class IMDSManagedIdentity extends AbstractManagedIdentity {

    // IMDS constants. Docs for IMDS are available here https://docs.microsoft.com/azure/active-directory/managed-identities-azure-resources/how-to-use-vm-token#get-a-token-using-http
    private final static Logger LOG = LoggerFactory.getLogger(IMDSManagedIdentity.class);
    private static URI DEFAULT_IMDS_ENDPOINT;

    static {
        try {
            DEFAULT_IMDS_ENDPOINT = new URI("http://169.254.169.254/metadata/identity/oauth2/token");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private String imdsTokenPath = "/metadata/identity/oauth2/token";
    private String imdsApiVersion = "2018-02-01";
    private static String defaultMessage = "[Managed Identity] Service request failed.";

    private static String identityUnavailableError = "[Managed Identity] Authentication unavailable. The requested identity has not been assigned to this resource.";
    private static String gatewayError = "[Managed Identity] Authentication unavailable. The request failed due to a gateway error.";

    private URI imdsEndpoint;

    public IMDSManagedIdentity(RequestContext requestContext) {
        super(requestContext, ManagedIdentitySourceType.Imds);
        if (!StringHelper.isNullOrBlank(EnvironmentVariables.getAzurePodIdentityAuthorityHost())){
            LOG.info("[Managed Identity] Environment variable AZURE_POD_IDENTITY_AUTHORITY_HOST for IMDS returned endpoint: " + EnvironmentVariables.getAzurePodIdentityAuthorityHost());
            try {
                imdsEndpoint = new URI(EnvironmentVariables.getAzurePodIdentityAuthorityHost());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }

            StringBuilder builder = new StringBuilder(EnvironmentVariables.getAzurePodIdentityAuthorityHost());
            builder.append("/" + imdsTokenPath);
            try {
                URI imdsEndpoint = new URI(builder.toString());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        else
        {
            LOG.info("[Managed Identity] Unable to find AZURE_POD_IDENTITY_AUTHORITY_HOST environment variable for IMDS, using the default endpoint.");
            imdsEndpoint = DEFAULT_IMDS_ENDPOINT;
        }

        LOG.info("[Managed Identity] Creating IMDS managed identity source. Endpoint URI: " + imdsEndpoint);
    }

    @Override
    public ManagedIdentityRequest createRequest(String resource) {
        ManagedIdentityRequest request = new ManagedIdentityRequest(HttpMethod.GET, imdsEndpoint);

        Map<String, String> headers = new HashMap<>();
        headers.put("Metadata", "true");
        request.headers = headers;

        Map<String, String> queryParameters = new HashMap<>();
        queryParameters.put("api-version",imdsApiVersion);
        queryParameters.put("resource", resource);

        String clientId = getManagedIdentityUserAssignedClientId();
        String resourceId = getManagedIdentityUserAssignedResourceId();
        if (!StringHelper.isNullOrBlank(clientId))
        {
            LOG.info("[Managed Identity] Adding user assigned client id to the request.");
            queryParameters.put(Constants.MANAGED_IDENTITY_CLIENT_ID, clientId);
        }

        if (!StringHelper.isNullOrBlank(resourceId))
        {
            LOG.info("[Managed Identity] Adding user assigned resource id to the request.");
            queryParameters.put(Constants.MANAGED_IDENTITY_RESOURCE_ID, resourceId);
        }

        request.queryParameters = queryParameters;
        return request;
    }

    @Override
    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            HTTPResponse response)
    {
        // handle error status codes indicating managed identity is not available
        String baseMessage;

        if(response.getStatusCode()== HttpURLConnection.HTTP_BAD_REQUEST){
            baseMessage = identityUnavailableError;
        }else if(response.getStatusCode()== HttpURLConnection.HTTP_BAD_GATEWAY ||
                response.getStatusCode()== HttpURLConnection.HTTP_GATEWAY_TIMEOUT){
            baseMessage = gatewayError;
        }else{
            baseMessage = null;
        }

        if (baseMessage != null)
        {
            String message = createRequestFailedMessage(response, baseMessage);

            String errorContentMessage = getMessageFromErrorResponse(response);

            message = message + " " + errorContentMessage;

            LOG.error("Error message: {message} Http status code: {response.StatusCode}");
            throw new MsalManagedIdentityException("managed_identity_request_failed", message,
                    ManagedIdentitySourceType.Imds);
        }

        // Default behavior to handle successful scenario and general errors.
        return handleResponse(parameters, response);
    }

    private static String createRequestFailedMessage(HTTPResponse response, String message)
    {
        StringBuilder messageBuilder = new StringBuilder();

        messageBuilder.append(StringHelper.isNullOrBlank(message) ? defaultMessage : message);
        messageBuilder.append("Status: ");
        messageBuilder.append(response.getStatusCode());

        if (response.getContent() != null)
        {
            messageBuilder.append("Content:").append(response.getContent());
        }

        messageBuilder.append("Headers:");

        for(String key : response.getHeaderMap().keySet())
        {
            messageBuilder.append(key).append(response.getHeaderValue(key));
        }

        return messageBuilder.toString();
    }
}
