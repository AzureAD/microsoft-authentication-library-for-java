// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

/**
 * Constants for lab API endpoints and query parameters.
 */
public class LabApiConstants {

    // Base endpoints
    public static final String LAB_ENDPOINT = "https://msidlab.com/api/user";
    public static final String LAB_USER_CREDENTIAL_ENDPOINT = "https://msidlab.com/api/LabSecret";
    public static final String LAB_APP_ENDPOINT = "https://msidlab.com/api/app/";
    public static final String LAB_INFO_ENDPOINT = "https://msidlab.com/api/Lab/";

    // Query parameter keys
    public static final String USER_TYPE = "usertype";
    public static final String MULTI_FACTOR_AUTHENTICATION = "mfa";
    public static final String PROTECTION_POLICY = "protectionpolicy";
    public static final String HOME_DOMAIN = "homedomain";
    public static final String B2C_PROVIDER = "b2cprovider";
    public static final String FEDERATION_PROVIDER = "federationprovider";
    public static final String AZURE_ENVIRONMENT = "azureenvironment";

    public static final String MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.com/";
    public static final String ARLINGTON_MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.us/";
    public static final String MICROSOFT_AUTHORITY_TENANT = "msidlab4.onmicrosoft.com";
    public static final String ARLINGTON_AUTHORITY_TENANT = "arlmsidlab1.onmicrosoft.us";

    public static final String ORGANIZATIONS_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "organizations/";
    public static final String MICROSOFT_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "microsoft.onmicrosoft.com";

    public static final String ARLINGTON_ORGANIZATIONS_AUTHORITY = ARLINGTON_MICROSOFT_AUTHORITY_HOST + "organizations/";
    public static final String ARLINGTON_GRAPH_DEFAULT_SCOPE = "https://graph.microsoft.us/.default";
    public static final String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";

    public static final String ARLINGTON_APP_ID = "cb7faed4-b8c0-49ee-b421-f5ed16894c83";
    public static final String LAB_API_SCOPE = "https://request.msidlab.com/.default";

}