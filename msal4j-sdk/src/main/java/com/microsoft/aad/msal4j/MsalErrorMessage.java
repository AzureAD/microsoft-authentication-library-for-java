// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

class MsalErrorMessage {

    public static final String MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR = "[Managed Identity] The environment variable %s contains an invalid Uri %s in %s managed identity source.";

    public static final String MANAGED_IDENTITY_NO_CHALLENGE_ERROR = "[Managed Identity] Did not receive expected WWW-Authenticate header in the response from Azure Arc Managed Identity Endpoint.";

    public static final String MANAGED_IDENTITY_INVALID_CHALLENGE = "[Managed Identity] The WWW-Authenticate header in the response from Azure Arc Managed Identity Endpoint did not match the expected format.";

    public static final String MANAGED_IDENTITY_UNEXPECTED_RESPONSE = "[Managed Identity] Unexpected exception occurred when parsing the response. See the inner exception for details.";

    public static final String MANAGED_IDENTITY_USER_ASSIGNED_NOT_CONFIGURABLE_AT_RUNTIME = "[Managed Identity] Service Fabric user assigned managed identity ClientId or ResourceId is not configurable at runtime.";

    public static final String MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED = "[Managed Identity] User assigned identity is not supported by the %s Managed Identity. To authenticate with the system assigned identity use ManagedIdentityApplication.builder(ManagedIdentityId.systemAssigned()).build().";

    public static final String SCOPES_REQUIRED = "At least one scope needs to be requested for this authentication flow. ";

    public static final String DEFAULT_MESSAGE = "[Managed Identity] Service request failed.";

    public static final String IDENTITY_UNAVAILABLE_ERROR = "[Managed Identity] Authentication unavailable. The requested identity has not been assigned to this resource.";
    public static final String GATEWAY_ERROR = "[Managed Identity] Authentication unavailable. The request failed due to a gateway error.";
}
