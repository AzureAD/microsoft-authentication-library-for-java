// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

final class Constants {

    static final String CACHE_KEY_SEPARATOR = "-";
    static final String SCOPES_SEPARATOR = " ";
    static final String POINT_DELIMITER = ".";

    static final int AAD_JWT_TOKEN_LIFETIME_SECONDS = 60 * 10;

    public static final String MANAGED_IDENTITY_CLIENT_ID = "client_id";
    public static final String MANAGED_IDENTITY_RESOURCE_ID = "mi_res_id";
    public static final String MANAGED_IDENTITY_OBJECT_ID = "object_id";
    public static final String MANAGED_IDENTITY_DEFAULT_TENTANT = "managed_identity";

    public static final String IDENTITY_ENDPOINT = "IDENTITY_ENDPOINT";
    public static final String IDENTITY_HEADER = "IDENTITY_HEADER";
    public static final String AZURE_POD_IDENTITY_AUTHORITY_HOST = "AZURE_POD_IDENTITY_AUTHORITY_HOST";
    public static final String IMDS_ENDPOINT = "IMDS_ENDPOINT";
    public static final String MSI_ENDPOINT = "MSI_ENDPOINT";
    public static final String IDENTITY_SERVER_THUMBPRINT = "IDENTITY_SERVER_THUMBPRINT";

}
