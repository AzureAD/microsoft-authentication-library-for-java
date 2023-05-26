package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

//base class for all sources that support managed identity
abstract class AbstractManagedIdentitySource {

    protected static final String TIMEOUT_ERROR = "[Managed Identity] Authentication unavailable. The request to the managed identity endpoint timed out.";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedIdentitySource.class);
    private static final String MANAGED_IDENTITY_NO_RESPONSE_RECEIVED = "[Managed Identity] Authentication unavailable. No response received from the managed identity endpoint.";
    public static final String MANAGED_IDENTITY_REQUEST_FAILED = "managed_identity_request_failed";

    protected final RequestContext requestContext;
    private ServiceBundle serviceBundle;
    private ManagedIdentitySourceType managedIdentitySourceType;

   @Getter
   @Setter
   private boolean isUserAssignedManagedIdentity;
   @Getter
   @Setter
   private String managedIdentityUserAssignedClientId;
    @Getter
    @Setter
    private String managedIdentityUserAssignedResourceId;

    public AbstractManagedIdentitySource(RequestContext requestContext, ServiceBundle serviceBundle,
                                         ManagedIdentitySourceType sourceType) {
        this.requestContext = requestContext;
        this.managedIdentitySourceType = sourceType;
        this.serviceBundle = serviceBundle;
    }

    public ManagedIdentityResponse getManagedIdentityResponse(
            ManagedIdentityParameters parameters) {

        // Convert the scopes to a resource string.
        String resource = parameters.getResource();

        ManagedIdentityRequest request = createManagedIdentityRequest(resource);

        OAuthHttpRequest oAuthHttpRequest = null;
        try {
            oAuthHttpRequest = createOAuthHttpRequest(request);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        HTTPResponse oauthHttpResponse = null;
        try {
            oauthHttpResponse = oAuthHttpRequest.send();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return handleResponse(parameters, oauthHttpResponse);
    }

    OAuthHttpRequest createOAuthHttpRequest(ManagedIdentityRequest managedIdentityRequest) throws SerializeException, MalformedURLException, ParseException {

        final OAuthHttpRequest oAuthHttpRequest;
        if(managedIdentityRequest.method.equals(HttpMethod.GET)){
            try {
                oAuthHttpRequest = new OAuthHttpRequest(
                        HTTPRequest.Method.GET,
                        managedIdentityRequest.computeURI(),
                        managedIdentityRequest.headers,
                        requestContext,
                        serviceBundle
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }else{
            try {
                oAuthHttpRequest = new OAuthHttpRequest(
                        HTTPRequest.Method.POST,
                        managedIdentityRequest.computeURI(),
                        managedIdentityRequest.headers,
                        requestContext,
                        serviceBundle
                );
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }

        oAuthHttpRequest.setContentType(HTTPContentType.ApplicationURLEncoded.contentType);
        return oAuthHttpRequest;
    }

    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            HTTPResponse response) {

        String message;

        try {
            if (response.getStatusCode() == HttpURLConnection.HTTP_OK) {
                LOG.info("[Managed Identity] Successful response received.");
                return getSuccessfulResponse(response);
            } else {
                message = getMessageFromErrorResponse(response);
                LOG.error(
                        String.format("[Managed Identity] request failed, HttpStatusCode: %s Error message: %s",
                                response.getStatusCode(), message));
                throw new MsalManagedIdentityException(MANAGED_IDENTITY_REQUEST_FAILED, message, managedIdentitySourceType);
            }
        } catch (Exception e) {
            if (!(e instanceof MsalServiceException)) {
                LOG.error(
                        String.format("[Managed Identity] Exception: %s Http status code: %s", e.getMessage(),
                                response != null ? response.getStatusCode() : ""));
                message = MsalErrorMessage.MANAGED_IDENTITY_UNEXPECTED_RESPONSE;
            } else {
                throw e;
            }
            throw new MsalManagedIdentityException(MANAGED_IDENTITY_REQUEST_FAILED, message, managedIdentitySourceType);
        }
    }

    public abstract ManagedIdentityRequest createManagedIdentityRequest(String resource);

    protected ManagedIdentityResponse getSuccessfulResponse(HTTPResponse response) {

        ManagedIdentityResponse managedIdentityResponse = JsonHelper
                .convertJsonToObject(response.getContent(), ManagedIdentityResponse.class);

        if (managedIdentityResponse == null || managedIdentityResponse.getAccessToken() == null
                || managedIdentityResponse.getAccessToken().isEmpty() || managedIdentityResponse.getExpiresOn() == null
                || managedIdentityResponse.getExpiresOn().isEmpty()) {
            LOG.error("[Managed Identity] Response is either null or insufficient for authentication.");

        }

        return managedIdentityResponse;
    }

    protected String getMessageFromErrorResponse(HTTPResponse response){
        ManagedIdentityErrorResponse managedIdentityErrorResponse =
                JsonHelper.convertJsonToObject(response.getContent(), ManagedIdentityErrorResponse.class);

        if (managedIdentityErrorResponse == null) {
            return MANAGED_IDENTITY_NO_RESPONSE_RECEIVED;
        }

        if (managedIdentityErrorResponse.getMessage() != null && !managedIdentityErrorResponse.getMessage().isEmpty()) {
            return String.format("[Managed Identity] Error Message: %s Managed Identity Correlation ID: %s Use this Correlation ID for further investigation.",
                    managedIdentityErrorResponse.getMessage(), managedIdentityErrorResponse.getCorrelationId());
        }

        return String.format("[Managed Identity] Error Code: %s Error Message: %s",
                managedIdentityErrorResponse.getError(), managedIdentityErrorResponse.getErrorDescription());

    }
}
