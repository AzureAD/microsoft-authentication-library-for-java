// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public class MsalErrorMessage {

    public static final String MANAGED_IDENTITY_ENDPOINT_INVALID_URI_ERROR = "[Managed Identity] The environment variable {0} contains an invalid Uri {1} in {2} managed identity source.";

    public static final String MANAGED_IDENTITY_NO_CHALLENGE_ERROR = "[Managed Identity] Did not receive expected WWW-Authenticate header in the response from Azure Arc Managed Identity Endpoint.";

    public static final String MANAGED_IDENTITY_INVALID_CHALLENGE = "[Managed Identity] The WWW-Authenticate header in the response from Azure Arc Managed Identity Endpoint did not match the expected format.";

    public static final String MANAGED_IDENTITY_UNEXPECTED_RESPONSE = "[Managed Identity] Unexpected exception occurred when parsing the response. See the inner exception for details.";

    public static final String MANAGED_IDENTITY_ENPOINT_INVALID_URI_ERROR = "[Managed Identity] The environment variable {0} contains an invalid Uri {1} in {2} managed identity source.";

    public static final String MANAGED_IDENTITY_USER_ASSIGNED_NOT_CONFIGURABLE_AT_RUNTIME = "[Managed Identity] Service Fabric user assigned managed identity ClientId or ResourceId is not configurable at runtime.";

    public static final String MANAGED_IDENTITY_USER_ASSIGNED_NOT_SUPPORTED = "[Managed Identity] User assigned identity is not supported by the {0} Managed Identity. To authenticate with the system assigned identity omit the client id in ManagedIdentityApplicationBuilder.Create().";

    public static final String SCOPES_REQUIRED = "At least one scope needs to be requested for this authentication flow. ";
}
