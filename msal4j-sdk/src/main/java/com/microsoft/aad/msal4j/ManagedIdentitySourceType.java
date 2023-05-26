// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

public enum ManagedIdentitySourceType {
    /// Default.
    None,
    /// The source to acquire token for managed identity is IMDS.
    Imds,
    /// The source to acquire token for managed identity is App Service.
    AppService,
    /// The source to acquire token for managed identity is Azure Arc.
    AzureArc,
    /// The source to acquire token for managed identity is Cloud Shell.
    CloudShell,
    /// The source to acquire token for managed identity is Service Fabric.
    ServiceFabric
}
