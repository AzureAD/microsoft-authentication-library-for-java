// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URISyntaxException;

//base class for all sources that support managed identity
abstract class AbstractManagedIdentitySource {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedIdentitySource.class);
    private static final String MANAGED_IDENTITY_NO_RESPONSE_RECEIVED = "[Managed Identity] Authentication unavailable. No response received from the managed identity endpoint.";

    protected final ManagedIdentityRequest managedIdentityRequest;
    protected final ServiceBundle serviceBundle;
    ManagedIdentitySourceType managedIdentitySourceType;
    ManagedIdentityIdType idType;
    String userAssignedId;

    private boolean isUserAssignedManagedIdentity;

    private String managedIdentityUserAssignedClientId;

    private String managedIdentityUserAssignedResourceId;

    public AbstractManagedIdentitySource(MsalRequest msalRequest, ServiceBundle serviceBundle,
                                         ManagedIdentitySourceType sourceType) {
        this.managedIdentityRequest = (ManagedIdentityRequest) msalRequest;
        this.managedIdentitySourceType = sourceType;
        this.serviceBundle = serviceBundle;
        this.idType = ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getIdType();
        this.userAssignedId = ((ManagedIdentityApplication) msalRequest.application()).getManagedIdentityId().getUserAssignedId();
    }

    /**
     * TODO: Add field description
     */
    public ManagedIdentityResponse getManagedIdentityResponse(
            ManagedIdentityParameters parameters) {

        createManagedIdentityRequest(parameters.resource);
        managedIdentityRequest.addTokenRevocationParametersToQuery(parameters);
        IHttpResponse response;

        try {
            HttpRequest httpRequest = new HttpRequest(managedIdentityRequest.method,
                            managedIdentityRequest.computeURI().toString(),
                            managedIdentityRequest.headers);
            response = serviceBundle.getHttpHelper().executeHttpRequest(httpRequest, managedIdentityRequest.requestContext(), serviceBundle);
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

    /**
     * TODO: Add field description
     */
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
                        String.format("[Managed Identity] request failed, HttpStatusCode: %s, Error message: %s",
                                response.statusCode(), message));
                throw new MsalServiceException(message, AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, managedIdentitySourceType);
            }
        } catch (Exception e) {
            if (!(e instanceof MsalServiceException)) {
                message = String.format("[Managed Identity] Unexpected exception occurred when parsing the response, HttpStatusCode: %s, Error message: %s",
                        response.statusCode(), e.getMessage());
            } else {
                throw e;
            }
            throw new MsalServiceException(message, AuthenticationErrorCode.MANAGED_IDENTITY_REQUEST_FAILED, managedIdentitySourceType);
        }
    }

    /**
     * TODO: Add description
     */
    public abstract void createManagedIdentityRequest(String resource);

    protected ManagedIdentityResponse getSuccessfulResponse(IHttpResponse response) {

        ManagedIdentityResponse managedIdentityResponse;
        try {
            managedIdentityResponse = JsonHelper.convertJsonStringToJsonSerializableObject(response.body(), ManagedIdentityResponse::fromJson);
        } catch (MsalJsonParsingException e) {
            throw new MsalJsonParsingException(String.format(MsalErrorMessage.MANAGED_IDENTITY_RESPONSE_PARSE_FAILURE, response.statusCode(), e.getMessage()), MsalError.MANAGED_IDENTITY_RESPONSE_PARSE_FAILURE, managedIdentitySourceType);
        }

        if (managedIdentityResponse == null || managedIdentityResponse.getAccessToken() == null
                || managedIdentityResponse.getAccessToken().isEmpty() || managedIdentityResponse.getExpiresOn() == null
                || managedIdentityResponse.getExpiresOn().isEmpty()) {
            throw new MsalServiceException("[Managed Identity] Response is either null or insufficient for authentication.", MsalError.MANAGED_IDENTITY_REQUEST_FAILED, managedIdentitySourceType);
        }

        return managedIdentityResponse;
    }

    protected String getMessageFromErrorResponse(IHttpResponse response) {

        ManagedIdentityErrorResponse managedIdentityErrorResponse;
        try {
            managedIdentityErrorResponse = JsonHelper.convertJsonToObject(response.body(), ManagedIdentityErrorResponse.class);
        } catch (MsalJsonParsingException e) {
            throw new MsalJsonParsingException(String.format(MsalErrorMessage.MANAGED_IDENTITY_RESPONSE_PARSE_FAILURE, response.statusCode(), e.getMessage()), MsalError.MANAGED_IDENTITY_RESPONSE_PARSE_FAILURE, managedIdentitySourceType);
        }

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

    protected static IEnvironmentVariables getEnvironmentVariables() {
        return ManagedIdentityApplication.environmentVariables == null ?
                new EnvironmentVariables() : ManagedIdentityApplication.environmentVariables;
    }

    /**
     * Checks if user assigned managed identity.
     * 
     * @return true if user assigned managed identity, false otherwise
     */
    public boolean isUserAssignedManagedIdentity() {
        return this.isUserAssignedManagedIdentity;
    }

    /**
     * Gets the managed identity user assigned client id.
     * 
     * @return the managed identity user assigned client id
     */
    public String getManagedIdentityUserAssignedClientId() {
        return this.managedIdentityUserAssignedClientId;
    }

    /**
     * Gets the managed identity user assigned resource id.
     * 
     * @return the managed identity user assigned resource id
     */
    public String getManagedIdentityUserAssignedResourceId() {
        return this.managedIdentityUserAssignedResourceId;
    }

    /**
     * Sets the user assigned managed identity.
     * 
     * @param isUserAssignedManagedIdentity the user assigned managed identity to set
     */
    public void setUserAssignedManagedIdentity(boolean isUserAssignedManagedIdentity) {
        this.isUserAssignedManagedIdentity = isUserAssignedManagedIdentity;
    }

    /**
     * Sets the managed identity user assigned client id.
     * 
     * @param managedIdentityUserAssignedClientId the managed identity user assigned client id to set
     */
    public void setManagedIdentityUserAssignedClientId(String managedIdentityUserAssignedClientId) {
        this.managedIdentityUserAssignedClientId = managedIdentityUserAssignedClientId;
    }

    /**
     * Sets the managed identity user assigned resource id.
     * 
     * @param managedIdentityUserAssignedResourceId the managed identity user assigned resource id to set
     */
    public void setManagedIdentityUserAssignedResourceId(String managedIdentityUserAssignedResourceId) {
        this.managedIdentityUserAssignedResourceId = managedIdentityUserAssignedResourceId;
    }
}
