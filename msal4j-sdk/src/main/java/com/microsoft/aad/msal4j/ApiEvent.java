// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Locale;

import static com.microsoft.aad.msal4j.TelemetryConstants.EVENT_NAME_PREFIX;

class ApiEvent extends Event {
    private static final String API_ID_KEY = EVENT_NAME_PREFIX + "api_id";
    private static final String AUTHORITY_KEY = EVENT_NAME_PREFIX + "authority";
    private static final String AUTHORITY_TYPE_KEY = EVENT_NAME_PREFIX + "authority_type";
    private static final String TENANT_ID_KEY = EVENT_NAME_PREFIX + "tenant_id";
    private static final String USER_ID_KEY = EVENT_NAME_PREFIX + "user_id";
    private static final String WAS_SUCCESSFUL_KEY = EVENT_NAME_PREFIX + "was_succesful";
    private static final String CORRELATION_ID_KEY = EVENT_NAME_PREFIX + "correlation_id";
    private static final String REQUEST_ID_KEY = EVENT_NAME_PREFIX + "request_id";
    private static final String IS_CONFIDENTIAL_CLIENT_KEY = EVENT_NAME_PREFIX + "is_confidential_client";
    private static final String API_ERROR_CODE_KEY = EVENT_NAME_PREFIX + "api_error_code";

    private Boolean logPii;

    public ApiEvent(Boolean logPii) {
        super(TelemetryConstants.API_EVENT_NAME_KEY);
        this.logPii = logPii;
    }

    /**
     * Sets the api id.
     * 
     * @param apiId the api id to set
     */
    public void setApiId(int apiId) {
        this.put(API_ID_KEY, Integer.toString(apiId).toLowerCase(Locale.ROOT));
    }

    /**
     * Sets the authority.
     * 
     * @param authority the authority to set
     */
    public void setAuthority(URI authority) {
        this.put(AUTHORITY_KEY, scrubTenant(authority));
    }

    /**
     * Sets the authority type.
     * 
     * @param authorityType the authority type to set
     */
    public void setAuthorityType(String authorityType) {
        this.put(AUTHORITY_TYPE_KEY, authorityType.toLowerCase(Locale.ROOT));
    }

    /**
     * Sets the tenant id.
     * 
     * @param tenantId the tenant id to set
     */
    public void setTenantId(String tenantId) {
        if (!StringHelper.isBlank(tenantId) && logPii) {
            this.put(TENANT_ID_KEY, StringHelper.createBase64EncodedSha256Hash(tenantId));
        } else {
            this.put(TENANT_ID_KEY, null);
        }
    }

    /**
     * Sets the account id.
     * 
     * @param accountId the account id to set
     */
    public void setAccountId(String accountId) {
        if (!StringHelper.isBlank(accountId) && logPii) {
            this.put(USER_ID_KEY, StringHelper.createBase64EncodedSha256Hash(accountId));
        } else {
            this.put(USER_ID_KEY, null);
        }
    }

    /**
     * Sets the was successful.
     * 
     * @param wasSuccessful the was successful to set
     */
    public void setWasSuccessful(boolean wasSuccessful) {
        this.put(WAS_SUCCESSFUL_KEY, String.valueOf(wasSuccessful).toLowerCase(Locale.ROOT));
    }

    /**
     * Gets the was successful.
     * 
     * @return the was successful
     */
    public boolean getWasSuccessful() {
        return Boolean.valueOf(this.get(WAS_SUCCESSFUL_KEY));
    }

    /**
     * Sets the correlation id.
     * 
     * @param correlationId the correlation id to set
     */
    public void setCorrelationId(String correlationId) {
        this.put(CORRELATION_ID_KEY, correlationId);
    }

    /**
     * Sets the request id.
     * 
     * @param requestId the request id to set
     */
    public void setRequestId(String requestId) {
        this.put(REQUEST_ID_KEY, requestId);
    }

    /**
     * Sets the is confidential client.
     * 
     * @param isConfidentialClient the is confidential client to set
     */
    public void setIsConfidentialClient(boolean isConfidentialClient) {
        this.put(IS_CONFIDENTIAL_CLIENT_KEY, String.valueOf(isConfidentialClient).toLowerCase(Locale.ROOT));
    }

    /**
     * Sets the api error code.
     * 
     * @param apiErrorCode the api error code to set
     */
    public void setApiErrorCode(String apiErrorCode) {
        this.put(API_ERROR_CODE_KEY, apiErrorCode);
    }
}
