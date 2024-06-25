// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class TestConstants {
    public final static String KEYVAULT_DEFAULT_SCOPE = "https://vault.azure.net/.default";
    public final static String MSIDLAB_DEFAULT_SCOPE = "https://request.msidlab.com/.default";
    public final static String MSIDLAB_VAULT_URL = "https://msidlabs.vault.azure.net/";
    public final static String MSIDLAB_CLIENT_ID = "f62c5ae3-bf3a-4af5-afa8-a68b800396e9";
    public final static String GRAPH_DEFAULT_SCOPE = "https://graph.windows.net/.default";
    public final static String USER_READ_SCOPE = "user.read";
    public final static String DEFAULT_SCOPE = ".default";
    public final static String B2C_LAB_SCOPE = "https://msidlabb2c.onmicrosoft.com/msaapp/user_impersonation";
    public final static String B2C_CONFIDENTIAL_CLIENT_APP_SECRETID = "MSIDLABB2C-MSAapp-AppSecret";
    public final static String B2C_CONFIDENTIAL_CLIENT_LAB_APP_ID = "MSIDLABB2C-MSAapp-AppID";

    public final static String MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.com/";
    public final static String MICROSOFT_AUTHORITY_BASIC_HOST = "login.microsoftonline.com";
    public final static String MICROSOFT_AUTHORITY_HOST_WITH_PORT = "https://login.microsoftonline.com:443/";
    public final static String ARLINGTON_MICROSOFT_AUTHORITY_HOST = "https://login.microsoftonline.us/";
    public final static String MICROSOFT_AUTHORITY_TENANT = "msidlab4.onmicrosoft.com";
    public final static String ARLINGTON_AUTHORITY_TENANT = "arlmsidlab1.onmicrosoft.us";

    public final static String ORGANIZATIONS_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "organizations/";
    public final static String COMMON_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "common/";
    public final static String CONSUMERS_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "consumers/";
    public final static String COMMON_AUTHORITY_WITH_PORT = MICROSOFT_AUTHORITY_HOST_WITH_PORT + "msidlab4.onmicrosoft.com";
    public final static String MICROSOFT_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "microsoft.onmicrosoft.com";
    public final static String TENANT_SPECIFIC_AUTHORITY = MICROSOFT_AUTHORITY_HOST + MICROSOFT_AUTHORITY_TENANT;
    public final static String REGIONAL_MICROSOFT_AUTHORITY_BASIC_HOST_WESTUS = "westus.login.microsoft.com";

    public final static String REGIONAL_MICROSOFT_AUTHORITY_BASIC_HOST_EASTUS = "eastus.login.microsoft.com";

//    public final static String CIAM_AUTHORITY = MICROSOFT_AUTHORITY_HOST + "msidlabciam1.onmicrosoft.com";
    public final static String CIAM_AUTHORITY = "https://msidlabciam1.ciamlogin.com/" + "msidlabciam1.onmicrosoft.com";

    public final static String CIAM_TEST_AUTHORITY = "https://contoso0781.ciamlogin.com/6babcaad-604b-40ac-a9d7-9fd97c0b779f/v2.0/.well-known/openid-configuration?dc=ESTS-PUB-EUS-AZ1-FD000-TEST1&ciamhost=true";

    public final static String ARLINGTON_ORGANIZATIONS_AUTHORITY = ARLINGTON_MICROSOFT_AUTHORITY_HOST + "organizations/";
    public final static String ARLINGTON_TENANT_SPECIFIC_AUTHORITY = ARLINGTON_MICROSOFT_AUTHORITY_HOST + ARLINGTON_AUTHORITY_TENANT;
    public final static String ARLINGTON_GRAPH_DEFAULT_SCOPE = "https://graph.microsoft.us/.default";

    public final static String B2C_AUTHORITY = "https://msidlabb2c.b2clogin.com/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_AUTHORITY_LEGACY_FORMAT = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/";

    public final static String B2C_ROPC_POLICY = "B2C_1_ROPC_Auth";
    public final static String B2C_SIGN_IN_POLICY = "B2C_1_SignInPolicy";
    public final static String B2C_AUTHORITY_SIGN_IN = B2C_AUTHORITY + B2C_SIGN_IN_POLICY;
    public final static String B2C_AUTHORITY_ROPC = B2C_AUTHORITY + B2C_ROPC_POLICY;
    public final static String B2C_READ_SCOPE = "https://msidlabb2c.onmicrosoft.com/msidlabb2capi/read";
    public final static String B2C_MICROSOFTLOGIN_AUTHORITY = "https://msidlabb2c.b2clogin.com/tfp/msidlabb2c.onmicrosoft.com/";
    public final static String B2C_MICROSOFTLOGIN_ROPC = B2C_MICROSOFTLOGIN_AUTHORITY + B2C_ROPC_POLICY;

    public final static String LOCALHOST = "http://localhost:";

    public final static String ADFS_AUTHORITY = "https://fs.msidlab8.com/adfs/";
    public final static String ADFS_SCOPE = USER_READ_SCOPE;
    public final static String ADFS_APP_ID = "PublicClientId";

    public final static String CLAIMS = "{\"id_token\":{\"auth_time\":{\"essential\":true}}}";
    public final static Set<String> CLIENT_CAPABILITIES_EMPTY = new HashSet<>(Collections.emptySet());
    public final static String AUTHORITY_PUBLIC_TENANT_SPECIFIC = "https://login.microsoftonline.com/" + MICROSOFT_AUTHORITY_TENANT;

    public final static String DEFAULT_ACCESS_TOKEN = "defaultAccessToken";
}
