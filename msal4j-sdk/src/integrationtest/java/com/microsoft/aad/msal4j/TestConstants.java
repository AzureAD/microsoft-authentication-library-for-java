// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public class TestConstants {
    public static final String KEYVAULT_DEFAULT_SCOPE = "https://vault.azure.net/.default";
    public static final String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";
    public static final String USER_READ_SCOPE = "user.read";
    public static final String DEFAULT_SCOPE = ".default";
    public static final String B2C_LAB_SCOPE = "https://msidlabb2c.onmicrosoft.com/msaapp/user_impersonation";
    public static final String B2C_CONFIDENTIAL_CLIENT_APP_SECRETID = "MSIDLABB2C-MSAapp-AppSecret";
    public static final String B2C_CONFIDENTIAL_CLIENT_LAB_APP_ID = "MSIDLABB2C-MSAapp-AppID";

    public static final String MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.com/";
    public static final String MICROSOFT_AUTHORITY_BASIC_HOST = "login.microsoftonline.com";
    public static final String MICROSOFT_AUTHORITY_HOST_WITH_PORT = "https://login.microsoftonline.com:443/";
    public static final String MICROSOFT_AUTHORITY_TENANT = "msidlab4.onmicrosoft.com";

    public static final String ORGANIZATIONS_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "organizations/";
    public static final String COMMON_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "common/";
    public static final String MICROSOFT_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "microsoft.onmicrosoft.com";
    public static final String REGIONAL_MICROSOFT_AUTHORITY_BASIC_HOST_WESTUS = "westus.login.microsoft.com";

    public static final String B2C_AUTHORITY = "https://msidlabb2c.b2clogin.com/msidlabb2c.onmicrosoft.com/";
    public static final String B2C_AUTHORITY_LEGACY_FORMAT = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/";

    public static final String B2C_ROPC_POLICY = "B2C_1_ROPC_Auth";
    public static final String B2C_SIGN_IN_POLICY = "B2C_1_SignInPolicy";
    public static final String B2C_AUTHORITY_SIGN_IN = B2C_AUTHORITY + B2C_SIGN_IN_POLICY;
    public static final String B2C_AUTHORITY_ROPC = B2C_AUTHORITY + B2C_ROPC_POLICY;
    public static final String B2C_READ_SCOPE = "https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read";
    public static final String B2C_MICROSOFTLOGIN_AUTHORITY = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/";
    public static final String B2C_MICROSOFTLOGIN_ROPC = B2C_MICROSOFTLOGIN_AUTHORITY + B2C_ROPC_POLICY;
    public static final String B2C_UPN = "b2clocal@msidlabb2c.onmicrosoft.com";

    public static final String LOCALHOST = "http://localhost:";

    public static final String ADFS_SCOPE = USER_READ_SCOPE;

    public static final String AUTHORITY_PUBLIC_TENANT_SPECIFIC = "https://login.microsoftonline.com/" + MICROSOFT_AUTHORITY_TENANT;

    public static final String ARLINGTON_TENANT_ID = "45ff0c17-f8b5-489b-b7fd-2fedebbec0c4";

    // Agentic / FMI / FIC integration test configuration (MSID Lab 4)
    public static final String AGENTIC_TENANT_ID = "10c419d4-4a50-45b2-aa4e-919fb84df24f";
    public static final String AGENTIC_BLUEPRINT_CLIENT_ID = "aab5089d-e764-47e3-9f28-cc11c2513821";
    public static final String AGENTIC_RMA_CLIENT_ID = "3bf56293-fbb5-42bd-a407-248ba7431a8c";
    public static final String AGENTIC_AGENT_APP_ID = "ab18ca07-d139-4840-8b3b-4be9610c6ed5";
    public static final String AGENTIC_USER_UPN = "agentuser1@id4slab1.onmicrosoft.com";
    public static final String AGENTIC_TOKEN_EXCHANGE_SCOPE = "api://AzureADTokenExchange/.default";
    public static final String AGENTIC_FMI_EXCHANGE_SCOPE = "api://AzureFMITokenExchange/.default";
    public static final String AGENTIC_GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    public static final String AGENTIC_WEB_API_SCOPE = "api://aa464f73-2868-4f67-b0e7-fc2f749e757f/.default";
    public static final String AGENTIC_AZURE_REGION = "westus3";
}
