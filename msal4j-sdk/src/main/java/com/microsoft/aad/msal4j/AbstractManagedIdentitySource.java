// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

//base class for all sources that support managed identity
abstract class AbstractManagedIdentitySource {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractManagedIdentitySource.class);
    private static final String MANAGED_IDENTITY_NO_RESPONSE_RECEIVED = "[Managed Identity] Authentication unavailable. No response received from the managed identity endpoint.";

    // IMDS (MSIv1) only supports this single custom claim. Any other top-level key causes IMDS to
    // return HTTP 400 with no useful diagnostic, so it is rejected client-side before the network call.
    private static final String XMS_AZ_NWPERIMID = "xms_az_nwperimid";

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

    public ManagedIdentityResponse getManagedIdentityResponse(
            ManagedIdentityParameters parameters) {

        createManagedIdentityRequest(parameters.resource);
        managedIdentityRequest.addTokenRevocationParametersToQuery(parameters);
        addClientClaimsToRequest(parameters);
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
     * Forwards client-originated claims (set via
     * {@link ManagedIdentityParameters.ManagedIdentityParametersBuilder#claimsFromClient(String)}) to
     * the managed identity endpoint. Only IMDS-based managed identity is supported; other sources fail
     * fast rather than silently dropping the value (which would also pollute the cache with a key the
     * endpoint never saw). For IMDS (a GET request) the claims are added as a query parameter; for any
     * POST-based source they would be added to the body.
     */
    private void addClientClaimsToRequest(ManagedIdentityParameters parameters) {
        if (StringHelper.isNullOrBlank(parameters.clientClaims)) {
            return;
        }

        if (managedIdentitySourceType != ManagedIdentitySourceType.IMDS) {
            throw new MsalClientException(
                    String.format("claimsFromClient is only supported for IMDS-based managed identity sources. "
                            + "The detected source is %s.", managedIdentitySourceType),
                    AuthenticationErrorCode.INVALID_REQUEST);
        }

        // IMDS == MSIv1: validate that the only top-level claim key is xms_az_nwperimid.
        validateMsiv1Claims(parameters.clientClaims);

        if (managedIdentityRequest.method == HttpMethod.GET) {
            if (managedIdentityRequest.queryParameters == null) {
                managedIdentityRequest.queryParameters = new HashMap<>();
            }
            // The value is URL-encoded later by StringHelper.serializeQueryParameters.
            managedIdentityRequest.queryParameters.put("claims", parameters.clientClaims);
            LOG.info("[Managed Identity] Adding client claims to IMDS request as query parameter.");
        } else {
            if (managedIdentityRequest.bodyParameters == null) {
                managedIdentityRequest.bodyParameters = new HashMap<>();
            }
            managedIdentityRequest.bodyParameters.put("claims", parameters.clientClaims);
            LOG.info("[Managed Identity] Adding client claims to request body.");
        }
    }

    private static void validateMsiv1Claims(String claimsJson) {
        Map<String, Object> parsed = JsonHelper.parseJsonToMap(claimsJson);
        for (String key : parsed.keySet()) {
            if (!XMS_AZ_NWPERIMID.equals(key)) {
                throw new MsalClientException(
                        String.format("MSIv1 (IMDS v1) only supports the `%s` custom claim. "
                                + "The claims JSON contained the unsupported key `%s`. "
                                + "Remove all keys other than `%s` when using claimsFromClient with MSIv1.",
                                XMS_AZ_NWPERIMID, key, XMS_AZ_NWPERIMID),
                        AuthenticationErrorCode.INVALID_REQUEST);
            }
        }
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
                LOG.error("[Managed Identity] request failed, HttpStatusCode: {}, Error message: {}",
                        response.statusCode(), message);
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
            managedIdentityErrorResponse = JsonHelper.convertJsonStringToJsonSerializableObject(response.body(), ManagedIdentityErrorResponse::fromJson);
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

    public boolean isUserAssignedManagedIdentity() {
        return this.isUserAssignedManagedIdentity;
    }

    public String getManagedIdentityUserAssignedClientId() {
        return this.managedIdentityUserAssignedClientId;
    }

    public String getManagedIdentityUserAssignedResourceId() {
        return this.managedIdentityUserAssignedResourceId;
    }

    public void setUserAssignedManagedIdentity(boolean isUserAssignedManagedIdentity) {
        this.isUserAssignedManagedIdentity = isUserAssignedManagedIdentity;
    }

    public void setManagedIdentityUserAssignedClientId(String managedIdentityUserAssignedClientId) {
        this.managedIdentityUserAssignedClientId = managedIdentityUserAssignedClientId;
    }

    public void setManagedIdentityUserAssignedResourceId(String managedIdentityUserAssignedResourceId) {
        this.managedIdentityUserAssignedResourceId = managedIdentityUserAssignedResourceId;
    }
}
