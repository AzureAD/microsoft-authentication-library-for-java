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

    /**
     * MSI v2: withAttestationSupport=true requires mtlsProofOfPossession=true.
     */
    public static final String MSI_V2_ATTESTATION_REQUIRES_POP = "msi_v2_attestation_requires_pop";

    /**
     * MSI v2 token acquisition failed (no silent fallback to MSI v1).
     */
    public static final String MSI_V2_ERROR = "msi_v2_error";

    /**
     * KeyGuard (VBS-backed hardware key) is not available on this platform.
     */
    public static final String MSI_V2_KEYGUARD_UNAVAILABLE = "msi_v2_keyguard_unavailable";
}
