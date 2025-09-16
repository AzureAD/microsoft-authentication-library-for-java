// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public final class TestConfiguration {

    public static final String AAD_HOST_NAME = "login.windows.net";
    public static final String AAD_TENANT_NAME = "aaltests.onmicrosoft.com";
    public static final String AAD_TENANT_ENDPOINT = "https://" + AAD_HOST_NAME
            + "/" + AAD_TENANT_NAME + "/";
    public static final String AAD_CLIENT_ID = "9083ccb8-8a46-43e7-8439-1d696df984ae";
    public static final String AAD_CLIENT_DUMMYSECRET = "client_dummysecret";
    public static final String AAD_MEX_RESPONSE_FILE = "/mex-response.xml";
    public static final String AAD_MEX_RESPONSE_FILE_INTEGRATED = "/mex-response-integrated.xml";
    public static final String AAD_MEX_2005_RESPONSE_FILE = "/mex-2005-response.xml";
    public static final String AAD_TOKEN_SUCCESS_FILE = "/token.xml";
    public static final String AAD_DEFAULT_REDIRECT_URI = "https://non_existing_uri.windows.com/";
    public static final String AAD_COMMON_AUTHORITY = "https://login.microsoftonline.com/common/";

    public static final String ADFS_HOST_NAME = "fs.ade2eadfs30.com";
    public static final String ADFS_TENANT_ENDPOINT = "https://"
            + ADFS_HOST_NAME + "/adfs/";

    public static final String B2C_HOST_NAME = "msidlabb2c.b2clogin.com";
    public static final String B2C_SIGN_IN_POLICY = "B2C_1_SignInPolicy";
    public static final String B2C_AUTHORITY = "https://" + B2C_HOST_NAME +
            "/tfp/msidlabb2c.onmicrosoft.com/" + B2C_SIGN_IN_POLICY;

    public static final String B2C_AUTHORITY_ENDPOINT = "https://" + B2C_HOST_NAME +
            "/msidlabb2c.onmicrosoft.com";
    public static final String B2C_AUTHORITY_CUSTOM_PORT = "https://login.microsoftonline.in:444/tfp/tenant/policy";
    public static final String B2C_AUTHORITY_CUSTOM_PORT_TAIL_SLASH = "https://login.microsoftonline.in:444/tfp/tenant/policy/";

    public static final String TOKEN_ENDPOINT_OK_RESPONSE_ID_AND_ACCESS = "{\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6I"
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
            + "7c4\",\"refresh_token\":\"refresh_token\",\"scope\":\"user_impersonation\",\""
            + "id_token\":\"eyJhbGciOiAiSFMyNTYiLCAidHlwIjogIkpXVCJ9.ew0KICAiYXVkIjogImI2YzY5YTM3LWRmOTYtNGRiMC05MDg4LTJhYjk2ZTFkO" +
            "DIxNSIsDQogICJpc3MiOiAiaHR0cHM6Ly9sb2dpbi5taWNyb3NvZnRvbmxpbmUuY29tL2Y2NDVhZDkyLWUzOGQtNGQxYS1iNTEwLWQxYjA5YTc0YThjYS" +
            "92Mi4wIiwNCiAgImlhdCI6IDE1Mzg1Mzg0MjIsDQogICJuYmYiOiAxNTM4NTM4NDIyLA0KICAiZXhwIjogMTUzODU0MjMyMiwNCiAgIm5hbWUiOiAiQ2x" +
            "vdWQgSURMQUIgQmFzaWMgVXNlciIsDQogICJvaWQiOiAiOWY0ODgwZDgtODBiYS00YzQwLTk3YmMtZjdhMjNjNzAzMDg0IiwNCiAgInByZWZlcnJlZF91" +
            "c2VybmFtZSI6ICJpZGxhYkBtc2lkbGFiNC5vbm1pY3Jvc29mdC5jb20iLA0KICAic3ViIjogIlk2WWtCZEhOTkxITm1US2VsOUtoUno4d3Jhc3hkTFJGa" +
            "VAxNEJSUFdybjQiLA0KICAidGlkIjogImY2NDVhZDkyLWUzOGQtNGQxYS1iNTEwLWQxYjA5YTc0YThjYSIsDQogICJ1dGkiOiAiNm5jaVgwMlNNa2k5azc" +
            "zLUYxc1pBQSIsDQogICJ2ZXIiOiAiMi4wIg0KfQ==.e30=\", \"client_info\": \"eyJ1a" +
            "WQiOiI5ZjQ4ODBkOC04MGJhLTRjNDAtOTdiYy1mN2EyM2M3MDMwODQiLCJ1dGlkIjoiZjY0NWFkOTItZTM4ZC00ZDFhLWI1MTAtZDFiMDlhNzRhOGNhIn0\"" +
            "}";

    public static final String TOKEN_ENDPOINT_OK_RESPONSE_ACCESS_ONLY = "{\"token_type\":\"Bearer\",\"expires_in\":3600," +
            "\"access_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Ik5HVEZ2ZEstZnl0aEV1THdqcHdBSk9NOW4tQSJ9" +
            ".eyJhdWQiOiJiN2E2NzFkOC1hNDA4LTQyZmYtODZlMC1hYWY0NDdmZDE3YzQiLCJpc3MiOiJodRwczovL3N0cy53aW5kb3dzLm5ldC8" +
            "zMGJhYTY2Ni04ZGY4LTQ4ZTctOTdlNi03N2NmZDA5OTU5NjMvIiwiaWF0IjoxMzkzODQ0NTA0LCJuYmYiOjEzOTM4NDQ1MDQsImV4cC" +
            "I6MTM5Mzg0ODQwNCwidmVyIjoiMS4wIiwidGlkIjoiMzBiYWE2NjYtOGRmOC00OGU3LTk3ZTYtNzdjZmQwOTk1OTYzIiwib2lkIjoiN" +
            "GY4NTk5ODktYTJmZi00MTFlLTkwNDgtYzMyMjI0N2FjNjJjIiwidXBuIjoiYWRtaW5AYWFsdGVzdHMub25taWNyb3NvZnQuY29tIiwi" +
            "dW5pcXVlX25hbWUiOiJhZG1pbkBhYWx0ZXN0cy5vbm1pY3Jvc29mdC5jb20iLCJzdWIiOiJqQ0ttUENWWEFzblN1MVBQUzRWblo4c2V" +
            "ONTR3U3F0cW1RkpGbW14SEF3IiwiZmFtaWx5X25hbWUiOiJBZG1pbiIsImdpdmVuX25hbWUiOiJBREFMVGVzdHMiLCJhcHBpZCI6Ijk" +
            "wODNjY2I4LThhNDYtNDNlNy04NDM5LTFkNjk2ZGY5ODRhZSIsImFwcGlkYWNyIjoiMSIsInNjcCI6InVzZXJfaW1wZXJzb25hdGlvbi" +
            "IsImFjciI6IjEifQ.lUfDlkLdNGuAGUukgTnS_uWeSFXljbhId1l9PDrr7AwOSbOzogLvO14TaU294T6HeOQ8e0dUAvxEAMvsK_800A" +
            "-AGNvbHK363xDjgmu464ye3CQvwq73GoHkzuxILCJKo0DUj0_XsCpQ4TdkPepuhzaGc-zYsfMU1POuIOB87pzW7e_VDpCdxcN1fuk-7" +
            "CECPQb8nrO0L8Du8y-TZDdTSe-i_A0Alv48Zll-6tDY9cxfAR0UyYKl_Kf45kuHAphCWwPsjUxv4rGHhgXZPRlKFq7bkXP2Es4ixCQz" +
            "b3bVLLrtQaZjkQ1yn37ngJro8NR63EbHHjHTA9lRmf8KIQ\"" +
            "}";

    public static final String HTTP_ERROR_RESPONSE = "{\"error\":\"invalid_request\",\"error_description\":\"AADSTS90011: Request "
            + "is ambiguous, multiple application identifiers found. Application identifiers: 'd09bb6da-4d46-4a16-880c-7885d8291fb9"
            + ", 216ef81d-f3b2-47d4-ad21-a4df49b56dee'.\r\nTrace ID: 428a1f68-767d-4a1c-ae8e-f710eeaf4e9b\r\nCorrelation ID: 1e0955"
            + "88-68e4-4bb4-a54e-71ad81e7f013\r\nTimestamp: 2014-03-11 20:19:02Z\"}";

    public static final String TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE = "{\"error\":\"invalid_grant\"," +
            "\"error_description\":\"AADSTS65001: description\\r\\nCorrelation ID: 3a...5a\\r\\nTimestamp:2017-07-15 02:35:05Z\"," +
            "\"error_codes\":[50076]," +
            "\"timestamp\":\"2017-07-15 02:35:05Z\"," +
            "\"trace_id\":\"0788...000\"," +
            "\"correlation_id\":\"3a...95a\"," +
            "\"suberror\":\"basic_action\"}";

    public static final String CLAIMS_REQUEST = "{\"id_token\":{\"acr\":{\"values\":[\"urn:mace:incommon:iap:silver\",\"urn:mace:incommon:iap:bronze\"]},\"sub\":{\"essential\":true,\"value\":\"248289761001\"},\"auth_time\":{}},\"access_token\":{\"given_name\":{\"essential\":true},\"email\":null}}";
    public static final String CLAIMS_CHALLENGE = "{\"access_token\":{\"nbf\":{\"essential\":true,\"value\":\"1701477303\"}}}";
    public static final String CLIENT_CAPABILITIES = "{\"access_token\":{\"xms_cc\":{\"values\":[\"cp1\"]}}}";
    public static final String MERGED_CLAIMS_AND_CAPABILITIES = "{\"access_token\":{\"nbf\":{\"value\":\"1701477303\",\"essential\":true},\"xms_cc\":{\"values\":[\"cp1\"]}}}";
    public static final String MERGED_CLAIMS_AND_CHALLENGE = "{\"access_token\":{\"nbf\":{\"value\":\"1701477303\",\"essential\":true}}}";
    public static final String MERGED_CLAIMS_CAPABILITIES_AND_CHALLENGE = "{\"access_token\":{\"nbf\":{\"value\":\"1701477303\",\"essential\":true},\"xms_cc\":{\"values\":[\"cp1\"]}}}";
}
