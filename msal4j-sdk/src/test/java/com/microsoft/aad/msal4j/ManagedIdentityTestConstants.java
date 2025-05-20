// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class ManagedIdentityTestConstants {
    // ID types
    static final String CLIENT_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    static final String RESOURCE_ID = "/subscriptions/ffa4aaa2-4444-4444-5555-e3ccedd3d046/resourcegroups/UAMI_group/providers/Microsoft.ManagedIdentityClient/userAssignedIdentities/UAMI";
    static final String OBJECT_ID = "593b2662-5af7-4a90-a9cb-5a9de615b82f";

    // Resources
    static final String RESOURCE = "https://management.azure.com";
    static final String RESOURCE_DEFAULT_SUFFIX = "https://management.azure.com/.default";

    // Endpoints
    static final String APP_SERVICE_ENDPOINT = "http://127.0.0.1:41564/msi/token";
    static final String IMDS_ENDPOINT = "http://169.254.169.254/metadata/identity/oauth2/token";
    static final String AZURE_ARC_ENDPOINT = "http://localhost:40342/metadata/identity/oauth2/token";
    static final String CLOUDSHELL_ENDPOINT = "http://localhost:40342/metadata/identity/oauth2/token";
    static final String SERVICE_FABRIC_ENDPOINT = "http://localhost:40342/metadata/identity/oauth2/token";

    // Example responses
    static final String RESPONSE_MALFORMED_JSON = "missing starting bracket \"access_token\":\"accesstoken\",\"token_type\":" + "\"Bearer\",\"client_id\":\"a bunch of problems}";
    static final String MSI_ERROR_RESPONSE_500 = "{\"statusCode\":\"500\",\"message\":\"An unexpected error occured while fetching the AAD Token.\",\"correlationId\":\"7d0c9763-ff1d-4842-a3f3-6d49e64f4513\"}";
    static final String CLOUDSHELL_ERROR_RESPONSE = "{\"error\":{\"code\":\"AudienceNotSupported\",\"message\":\"Audience user.read is not a supported MSI token audience.\"}}";
    static final String MSI_ERROR_RESPONSE_NORETRY = "{\"statusCode\":\"123\",\"message\":\"Not one of the retryable error responses\",\"correlationId\":\"7d0c9763-ff1d-4842-a3f3-6d49e64f4513\"}";
}