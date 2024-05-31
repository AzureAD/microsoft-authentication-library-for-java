// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

enum ManagedIdentitySourceType {
    // Default.
    NONE,
    // The source used to acquire token for managed identity is IMDS.
    IMDS,
    // The source used to acquire token for managed identity is App Service.
    APP_SERVICE,
    // The source used to acquire token for managed identity is Azure Arc.
    AZURE_ARC,
    // The source used to acquire token for managed identity is Cloud Shell.
    CLOUD_SHELL,
    // The source used to acquire token for managed identity is Service Fabric.
    SERVICE_FABRIC,
    // The source to acquire token for managed identity is defaulted to IMDS when no environment variables are set.
    DEFAULT_TO_IMDS
}
