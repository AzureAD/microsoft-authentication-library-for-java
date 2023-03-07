// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AppCredentialProvider;
import labapi.AzureEnvironment;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter()
public class Config {
    private String organizationsAuthority;
    private String tenantSpecificAuthority;
    private String graphDefaultScope;
    AppCredentialProvider appProvider;
    private String tenant;

    String azureEnvironment;

    Config(String azureEnvironment) {
        this.azureEnvironment = azureEnvironment;

        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                organizationsAuthority = TestConstants.ORGANIZATIONS_AUTHORITY;
                tenantSpecificAuthority = TestConstants.TENANT_SPECIFIC_AUTHORITY;
                graphDefaultScope = TestConstants.GRAPH_DEFAULT_SCOPE;
                appProvider = new AppCredentialProvider(azureEnvironment);
                tenant = TestConstants.MICROSOFT_AUTHORITY_TENANT;
                break;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                organizationsAuthority = TestConstants.ARLINGTON_ORGANIZATIONS_AUTHORITY;
                tenantSpecificAuthority = TestConstants.ARLINGTON_TENANT_SPECIFIC_AUTHORITY;
                graphDefaultScope = TestConstants.ARLINGTON_GRAPH_DEFAULT_SCOPE;
                appProvider = new AppCredentialProvider(azureEnvironment);
                tenant = TestConstants.ARLINGTON_AUTHORITY_TENANT;
                break;
            default:
                throw new UnsupportedOperationException("Azure Environment - " + azureEnvironment + " unsupported");
        }
    }
}
