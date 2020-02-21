// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public class TestConstants {
    public final static String KEYVAULT_DEFAULT_SCOPE = "https://vault.azure.net/.default";
    public final static String MSIDLAB_DEFAULT_SCOPE = "https://msidlab.com/.default";
    public final static String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";
    public final static String USER_READ_SCOPE = "user.read";
    public final static String B2C_LAB_SCOPE = "https://msidlabb2c.onmicrosoft.com/msaapp/user_impersonation";
    public final static String B2C_CONFIDENTIAL_CLIENT_APP_SECRET = "MSIDLABB2C-MSAapp-AppSecret";
    public final static String B2C_CONFIDENTIAL_CLIENT_LAB_APP_ID = "MSIDLABB2C-MSAapp-AppID";

    public final static String MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.com/";
    public final static String ORGANIZATIONS_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "organizations/";
    public final static String COMMON_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "common/";
    public final static String MICROSOFT_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "microsoft.onmicrosoft.com";

    public final static String B2C_AUTHORITY = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_AUTHORITY_URL = "https://msidlabb2c.b2clogin.com/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_ROPC_POLICY = "B2C_1_ROPC_Auth";
    public final static String B2C_SIGN_IN_POLICY = "B2C_1_SignInPolicy";
    public final static String B2C_AUTHORITY_SIGN_IN = B2C_AUTHORITY + B2C_SIGN_IN_POLICY;
    public final static String B2C_AUTHORITY_ROPC = B2C_AUTHORITY + B2C_ROPC_POLICY;
    public final static String B2C_READ_SCOPE = "https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read";
    public final static String B2C_MICROSOFTLOGIN_AUTHORITY = "https://login.microsoftonline.com/tfp/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_MICROSOFTLOGIN_ROPC = B2C_MICROSOFTLOGIN_AUTHORITY + B2C_ROPC_POLICY;

    public final static String LOCALHOST = "http://localhost:";
    public final static String LOCAL_FLAG_ENV_VAR = "MSAL_JAVA_RUN_LOCAL";

    public final static String ADFS_AUTHORITY = "https://fs.msidlab8.com/adfs/";
    public final static String ADFS_SCOPE = USER_READ_SCOPE;
    public final static String ADFS_APP_ID = "PublicClientId";
}
