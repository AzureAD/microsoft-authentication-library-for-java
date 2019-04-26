// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

public final class TestConfiguration {

    public final static String AAD_HOST_NAME = "login.windows.net";
    public final static String AAD_TENANT_NAME = "aaltests.onmicrosoft.com";
    public final static String AAD_TENANT_ENDPOINT = "https://" + AAD_HOST_NAME
            + "/" + AAD_TENANT_NAME + "/";
    public final static String AAD_CLIENT_ID = "9083ccb8-8a46-43e7-8439-1d696df984ae";
    public final static String AAD_CLIENT_SECRET = "client_secret";
    public final static String AAD_RESOURCE_ID = "b7a671d8-a408-42ff-86e0-aaf447fd17c4";
    public final static String AAD_CERTIFICATE_PATH = "/test-certificate.pfx";
    public final static String AAD_MEX_RESPONSE_FILE = "/mex-response.xml";
    public final static String AAD_MEX_RESPONSE_FILE_INTEGRATED = "/mex-response-integrated.xml";
    public final static String AAD_MEX_2005_RESPONSE_FILE = "/mex-2005-response.xml";
    public final static String AAD_TOKEN_ERROR_FILE = "/token-error.xml";
    public final static String AAD_TOKEN_SUCCESS_FILE = "/token.xml";
    public final static String AAD_CERTIFICATE_PASSWORD = "password";
    public final static String AAD_DEFAULT_REDIRECT_URI = "https://non_existing_uri.windows.com/";
    public final static String AAD_REDIRECT_URI_FOR_CONFIDENTIAL_CLIENT = "https://non_existing_uri_for_confidential_client.com/";

    public final static String ADFS_HOST_NAME = "fs.ade2eadfs30.com";
    public final static String ADFS_TENANT_ENDPOINT = "https://"
            + ADFS_HOST_NAME + "/adfs/";
    public final static String AAD_UNKNOWN_TENANT_ENDPOINT = "https://lgn.windows.net/"
            + AAD_TENANT_NAME + "/";
    public final static String B2C_HOST_NAME = "login.microsoftonline.com";
    public final static String B2C_TENANT_ENDPOINT = "https://" +  B2C_HOST_NAME +
            "/tfp/msidlabb2c.onmicrosoft.com/B2C_1_ROPC_Auth/";
    public final static String B2C_AUTHORITY_CUSTOM_PORT = "https://login.microsoftonline.in:444/tfp/tenant/policy";
    public final static String B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH = "https://login.microsoftonline.in:444/tfp/tenant/policy/";


    public static String INSTANCE_DISCOVERY_RESPONSE = "{" +
            "\"tenant_discovery_endpoint\":\"https://login.microsoftonline.com/organizations/v2.0/.well-known/openid-configuration\"," +
            "\"api-version\":\"1.1\"," +
            "\"metadata\":[{\"preferred_network\":\"login.microsoftonline.com\",\"preferred_cache\":\"login.windows.net\",\"aliases\":[\"login.microsoftonline.com\",\"login.windows.net\",\"login.microsoft.com\",\"sts.windows.net\"]},{\"preferred_network\":\"login.partner.microsoftonline.cn\",\"preferred_cache\":\"login.partner.microsoftonline.cn\",\"aliases\":[\"login.partner.microsoftonline.cn\",\"login.chinacloudapi.cn\"]},{\"preferred_network\":\"login.microsoftonline.de\",\"preferred_cache\":\"login.microsoftonline.de\",\"aliases\":[\"login.microsoftonline.de\"]},{\"preferred_network\":\"login.microsoftonline.us\",\"preferred_cache\":\"login.microsoftonline.us\",\"aliases\":[\"login.microsoftonline.us\",\"login.usgovcloudapi.net\"]},{\"preferred_network\":\"login-us.microsoftonline.com\",\"preferred_cache\":\"login-us.microsoftonline.com\",\"aliases\":[\"login-us.microsoftonline.com\"]}]}";

    public final static String AAD_PREFERRED_NETWORK_ENV_ALIAS = "login.microsoftonline.com";
    public final static String AAD_PREFERRED_CACHE__ENV_ALIAS = "login.windows.net";

    public final static String HTTP_RESPONSE_FROM_AUTH_CODE = "{\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6I"
            + "k5HVEZ2ZEstZnl0aEV1THdqcHdBSk9NOW4tQSJ9.eyJhdWQiOiJiN2E2NzFkOC1hNDA4LTQyZmYtODZlMC1hYWY0NDdmZDE3YzQiLCJpc3MiOiJod"
            + "HRwczovL3N0cy53aW5kb3dzLm5ldC8zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwiaWF0IjoxMzkzODQ0NTA0LCJuYmYiOj"
            + "EzOTM4NDQ1MDQsImV4cCI6MTM5Mzg0ODQwNCwidmVyIjoiMS4wIiwidGlkIjoiMzBiYWE2NjYtOGRmOC00OGU3LTk3ZTYtNzdjZmQwOTk1OTYzIiwi"
            + "b2lkIjoiNGY4NTk5ODktYTJmZi00MTFlLTkwNDgtYzMyMjI0N2FjNjJjIiwidXBuIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwidW"
            + "5pcXVlX25hbWUiOiJhZG1pbkBhYWx0ZXN0cy5vbm1pY3Jvc29mdC5jb20iLCJzdWIiOiJqQ0ttUENWWEFzblN1MVBQUzRWblo4c2VONTR3U3F0cW1R"
            + "MkpGbW14SEF3IiwiZmFtaWx5X25hbWUiOiJBZG1pbiIsImdpdmVuX25hbWUiOiJBREFMVGVzdHMiLCJhcHBpZCI6IjkwODNjY2I4LThhNDYtNDNlNy"
            + "04NDM5LTFkNjk2ZGY5ODRhZSIsImFwcGlkYWNyIjoiMSIsInNjcCI6InVzZXJfaW1wZXJzb25hdGlvbiIsImFjciI6IjEifQ.lUfDlkLdNGuAGUukg"
            + "TnS_uWeSFXljbhId1l9PDrr7AwOSbOzogLvO14TaU294T6HeOQ8e0dUAvxEAMvsK_800A-AGNvbHK363xDjgmu464ye3CQvwq73GoHkzuxILCJKo0D"
            + "Uj0_XsCpQ4TdkPepuhzaGc-zYsfMU1POuIOB87pzW7e_VDpCdxcN1fuk-7CECPQb8nrO0L8Du8y-TZDdTSe-i_A0Alv48Zll-6tDY9cxfAR0Uy"
            + "YKl_Kf45kuHAphCWwPsjUxv4rGHhgXZPRlKFq7bkXP2Es4ixCQzb3bVLLrtQaZjkQ1yn37ngJro8NR63EbHHjHTA9lRmf8KIQ\",\"token_type\""
            + ":\"Bearer\",\"expires_in\":3600,\"expires_on\":\"1393848404\",\"resource\":\"b7a671d8-a408-42ff-86e0-aaf447fd1"
            + "7c4\",\"refresh_token\":\"AwABAAAAvPM1KaPlrEqdFSBzjqfTGPW9BlsxWYtD0DS9hJNOPHPnq8QYbv6_FKJ3MxSHbPAIekKwJ04TnZI1NnRj"
            + "CMhphmsy5ZFjWtLy3WN2E67b3aW2MTQ9lN06B-HdRdU0Rxi9EalB8kAlgb92Ob0zuhB90zWm3RbxshOW0vkHS3lNAV6_LQ8fZKeLTB1AuuRgLXsy-9"
            + "2h0yYuEQw_Uvs80IbGx59j1z3hJrCMEMrqh-Hf42OnckN-uR113zMircfEOMm0qzhrQdtLleTHELS79B18647OiG6e8k8saVIvJmjpIuy79_aN-Bk5"
            + "PRkkab-QAwn_R68via4nK0zpKHBCl0xoEvK59mqerNDEKJNY168_FHDYPiZyECaZCdlWdEqd3dLohFlnKv9zc0CnuvB_QSQHHNScqWkX53s_Us9I45"
            + "QlRDaUPB89QXzQQaT2wZ8i3wTOb7mnEUMzrNrpsor6E4ckiviMx6WoepZmqQEZV-Yd-UUgAA\",\"scope\":\"user_impersonation\",\""
            + "id_token\":\"eyJhbGciOiAiSFMyNTYiLCAidHlwIjogIkpXVCJ9.ew0KICAiYXVkIjogImI2YzY5YTM3LWRmOTYtNGRiMC05MDg4LTJhYjk2ZTFkO" +
            "DIxNSIsDQogICJpc3MiOiAiaHR0cHM6Ly9sb2dpbi5taWNyb3NvZnRvbmxpbmUuY29tL2Y2NDVhZDkyLWUzOGQtNGQxYS1iNTEwLWQxYjA5YTc0YThjYS" +
            "92Mi4wIiwNCiAgImlhdCI6IDE1Mzg1Mzg0MjIsDQogICJuYmYiOiAxNTM4NTM4NDIyLA0KICAiZXhwIjogMTUzODU0MjMyMiwNCiAgIm5hbWUiOiAiQ2x" +
            "vdWQgSURMQUIgQmFzaWMgVXNlciIsDQogICJvaWQiOiAiOWY0ODgwZDgtODBiYS00YzQwLTk3YmMtZjdhMjNjNzAzMDg0IiwNCiAgInByZWZlcnJlZF91" +
            "c2VybmFtZSI6ICJpZGxhYkBtc2lkbGFiNC5vbm1pY3Jvc29mdC5jb20iLA0KICAic3ViIjogIlk2WWtCZEhOTkxITm1US2VsOUtoUno4d3Jhc3hkTFJGa" +
            "VAxNEJSUFdybjQiLA0KICAidGlkIjogImY2NDVhZDkyLWUzOGQtNGQxYS1iNTEwLWQxYjA5YTc0YThjYSIsDQogICJ1dGkiOiAiNm5jaVgwMlNNa2k5azc" +
            "zLUYxc1pBQSIsDQogICJ2ZXIiOiAiMi4wIg0KfQ==.e30=\", \"client_info\": \"eyJ1a" +
            "WQiOiI5ZjQ4ODBkOC04MGJhLTRjNDAtOTdiYy1mN2EyM2M3MDMwODQiLCJ1dGlkIjoiZjY0NWFkOTItZTM4ZC00ZDFhLWI1MTAtZDFiMDlhNzRhOGNhIn0\"" +
            "}";

    public final static String HTTP_ERROR_RESPONSE = "{\"error\":\"invalid_request\",\"error_description\":\"AADSTS90011: Request "
            + "is ambiguous, multiple application identifiers found. Application identifiers: 'd09bb6da-4d46-4a16-880c-7885d8291fb9"
            + ", 216ef81d-f3b2-47d4-ad21-a4df49b56dee'.\r\nTrace ID: 428a1f68-767d-4a1c-ae8e-f710eeaf4e9b\r\nCorrelation ID: 1e0955"
            + "88-68e4-4bb4-a54e-71ad81e7f013\r\nTimestamp: 2014-03-11 20:19:02Z\"}";
}
