package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.Encoder;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

//base class for all sources that support managed identity
abstract class AbstractManagedIdentitySource {

    protected static final String TIMEOUT_ERROR = "[Managed Identity] Authentication unavailable. The request to the managed identity endpoint timed out.";
    private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedIdentitySource.class);
    private static final String MANAGED_IDENTITY_NO_RESPONSE_RECEIVED = "[Managed Identity] Authentication unavailable. No response received from the managed identity endpoint.";

    protected final ManagedIdentityRequest managedIdentityRequest;
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

    public AbstractManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle,
                                         ManagedIdentitySourceType sourceType) {
        this.managedIdentityRequest = (ManagedIdentityRequest) msalRequest;
        this.managedIdentitySourceType = sourceType;
        this.serviceBundle = serviceBundle;
    }

    public ManagedIdentityResponse getManagedIdentityResponse(
            ManagedIdentityParameters parameters) {

        createManagedIdentityRequest(parameters.resource);
        IHttpResponse response;

        try {
            HttpRequest httpRequest = new HttpRequest(HttpMethod.GET, managedIdentityRequest.computeURI().toString(), managedIdentityRequest.headers);
            response = HttpHelper.executeHttpRequest(httpRequest, managedIdentityRequest.requestContext(), serviceBundle);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return handleResponse(parameters, response);
    }

    public ManagedIdentityResponse handleResponse(
            ManagedIdentityParameters parameters,
            IHttpResponse response) {

        String message;

        try {
            if (response.statusCode() == HttpURLConnection.HTTP_OK) {
                LOG.info("[Managed Identity] Successful response received.");
                return getSuccessfulResponse(response);
            } else {
                message = getMessageFromErrorResponse(response);
                LOG.error(
                        String.format("[Managed Identity] request failed, HttpStatusCode: %s Error message: %s",
                                response.statusCode(), message));
                throw new MsalManagedIdentityException(AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, message, managedIdentitySourceType);
            }
        } catch (Exception e) {
            if (!(e instanceof MsalServiceException)) {
                LOG.error(
                        String.format("[Managed Identity] Exception: %s Http status code: %s", e.getMessage(),
                                response != null ? response.statusCode() : ""));
                message = MsalErrorMessage.MANAGED_IDENTITY_UNEXPECTED_RESPONSE;
            } else {
                throw e;
            }
            throw new MsalManagedIdentityException(AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, message, managedIdentitySourceType);
        }
    }

    public abstract void createManagedIdentityRequest(String resource);

    protected ManagedIdentityResponse getSuccessfulResponse(IHttpResponse response) {

        ManagedIdentityResponse managedIdentityResponse = JsonHelper
                .convertJsonToObject(response.body(), ManagedIdentityResponse.class);

        if (managedIdentityResponse == null || managedIdentityResponse.getAccessToken() == null
                || managedIdentityResponse.getAccessToken().isEmpty() || managedIdentityResponse.getExpiresOn() == null
                || managedIdentityResponse.getExpiresOn().isEmpty()) {
            LOG.error("[Managed Identity] Response is either null or insufficient for authentication.");

        }

        return managedIdentityResponse;
    }

    protected String getMessageFromErrorResponse(IHttpResponse response) {
        ManagedIdentityErrorResponse managedIdentityErrorResponse =
                JsonHelper.convertJsonToObject(response.body(), ManagedIdentityErrorResponse.class);

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

    protected static IEnvironmentVariables getEnvironmentVariables(ManagedIdentityParameters parameters) {
        return parameters.environmentVariables == null ? new EnvironmentVariables() : parameters.environmentVariables;
    }
}
