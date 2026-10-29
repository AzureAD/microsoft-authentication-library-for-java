// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Error code returned as a property in MsalException.
 */
public class MsalError {

    /**
     * Invalid managed identity endpoint.
     */
    public static final String INVALID_MANAGED_IDENTITY_ENDPOINT = "invalid_managed_identity_endpoint";

    /**
     * User assigned managed identity is not supported for this source.
     */
    public static final String USER_ASSIGNED_MANAGED_IDENTITY_NOT_SUPPORTED = "user_assigned_managed_identity_not_supported";

    public static final String USER_ASSIGNED_MANAGED_IDENTITY_NOT_CONFIRMED = "user_assigned_managed_identity_not_confirmed";

    /**
     * Managed Identity error response was received.
     */
    public static final String MANAGED_IDENTITY_REQUEST_FAILED = "managed_identity_request_failed";

    /**
     * Resource is required to fetch a token using managed identity.
     */
    public static final String RESOURCE_REQUIRED_MANAGED_IDENTITY = "resource_required_managed_identity";

    /**
     * Managed Identity endpoint is not reachable.
     */
    public static final String MANAGED_IDENTITY_UNREACHABLE_NETWORK = "managed_identity_unreachable_network";

    public static final String MANAGED_IDENTITY_FILE_READ_ERROR = "managed_identity_file_read_error";

    public static final String MANAGED_IDENTITY_RESPONSE_PARSE_FAILURE = "managed_identity_response_parse_failure";

    public static final String MANAGED_IDENTITY_MTLS_PROVIDER_UNAVAILABLE =
            "managed_identity_mtls_provider_unavailable";

    public static final String MANAGED_IDENTITY_MTLS_TOKEN_TYPE_INVALID =
            "managed_identity_mtls_token_type_invalid";

    public static final String MANAGED_IDENTITY_MTLS_REQUEST_FAILED =
            "managed_identity_mtls_request_failed";

    public static final String MANAGED_IDENTITY_MTLS_UNSUPPORTED =
            "managed_identity_mtls_unsupported";

    public static final String MANAGED_IDENTITY_MTLS_HTTP_CLIENT_UNSUPPORTED =
            "managed_identity_mtls_http_client_unsupported";

    public static final String MANAGED_IDENTITY_MTLS_MINIMUM_STRENGTH_NOT_MET =
            "managed_identity_mtls_minimum_strength_not_met";
}
