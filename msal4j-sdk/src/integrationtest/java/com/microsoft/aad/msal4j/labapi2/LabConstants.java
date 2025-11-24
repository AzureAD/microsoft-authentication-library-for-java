// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

/**
 * Constants for lab API endpoints and query parameters.
 */
class LabConstants {

    // Base endpoints
    static final String LAB_ENDPOINT = "https://msidlab.com/api/user";
    static final String LAB_APP_ENDPOINT = "https://msidlab.com/api/app/";
    static final String LAB_INFO_ENDPOINT = "https://msidlab.com/api/Lab/";

    // Query parameter keys
    static final String USER_TYPE = "usertype";
    static final String MULTI_FACTOR_AUTHENTICATION = "mfa";
    static final String PROTECTION_POLICY = "protectionpolicy";
    static final String B2C_PROVIDER = "b2cprovider";
    static final String FEDERATION_PROVIDER = "federationprovider";
    static final String SIGN_IN_AUDIENCE = "SignInAudience";
    static final String AZURE_ENVIRONMENT = "azureenvironment";

    static final String MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.com/";
    static final String MICROSOFT_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "microsoft.onmicrosoft.com";

    static final String LAB_API_SCOPE = "https://request.msidlab.com/.default";
}