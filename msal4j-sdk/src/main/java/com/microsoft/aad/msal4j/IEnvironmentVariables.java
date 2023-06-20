// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public interface IEnvironmentVariables {
    String IDENTITY_ENDPOINT = "IDENTITY_ENDPOINT";
    public static final String IDENTITY_HEADER = "IDENTITY_HEADER";
    public static final String AZURE_POD_IDENTITY_AUTHORITY_HOST = "AZURE_POD_IDENTITY_AUTHORITY_HOST";
    public static final String IMDS_ENDPOINT = "IMDS_ENDPOINT";
    public static final String MSI_ENDPOINT = "MSI_ENDPOINT";
    public static final String IDENTITY_SERVER_THUMBPRINT = "IDENTITY_SERVER_THUMBPRINT";

    String getEnvironmentVariable(String envVariable);
}
