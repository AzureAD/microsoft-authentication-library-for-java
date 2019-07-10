// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URI;
import java.util.Locale;

import static com.microsoft.aad.msal4j.TelemetryConstants.EVENT_NAME_PREFIX;

class ApiEvent extends Event{
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

    public ApiEvent(Boolean logPii){
        super(TelemetryConstants.API_EVENT_NAME_KEY);
        this.logPii = logPii;
    }

    public void setApiId(int apiId){
        this.put(API_ID_KEY, Integer.toString(apiId).toLowerCase(Locale.ROOT));
    }

    public void setAuthority(URI authority){
        this.put(AUTHORITY_KEY, scrubTenant(authority));
    }

    public void setAuthorityType(String authorityType){
        this.put(AUTHORITY_TYPE_KEY, authorityType.toLowerCase(Locale.ROOT));
    }

    public void setTenantId(String tenantId){
        if(!StringHelper.isBlank(tenantId) && logPii){
            this.put(TENANT_ID_KEY, hashPii(tenantId));
        } else {
            this.put(TENANT_ID_KEY, null);
        }
    }

    public void setAccountId(String accountId){
        if(!StringHelper.isBlank(accountId) && logPii){
            this.put(USER_ID_KEY, hashPii(accountId));
        } else {
            this.put(USER_ID_KEY, null);
        }
    }

    public void setWasSuccessful(boolean wasSuccessful){
        this.put(WAS_SUCCESSFUL_KEY, String.valueOf(wasSuccessful).toLowerCase(Locale.ROOT));
    }

    public boolean getWasSuccessful(){
        return Boolean.valueOf(this.get(WAS_SUCCESSFUL_KEY));
    }

    public void setCorrelationId(String correlationId){
        this.put(CORRELATION_ID_KEY, correlationId);
    }

    public void setRequestId(String requestId){
        this.put(REQUEST_ID_KEY, requestId);
    }

    public void setIsConfidentialClient(boolean isConfidentialClient){
        this.put(IS_CONFIDENTIAL_CLIENT_KEY, String.valueOf(isConfidentialClient).toLowerCase(Locale.ROOT));
    }

    public void setApiErrorCode(String apiErrorCode){
        this.put(API_ERROR_CODE_KEY, apiErrorCode);
    }
}
