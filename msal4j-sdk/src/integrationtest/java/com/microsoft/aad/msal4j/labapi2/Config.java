// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class Config {
    private String commonAuthority;
    private String organizationsAuthority;
    private String graphDefaultScope;
    AppCredentialProvider appProvider;
    private String tenant;

    String azureEnvironment;

    public Config(String azureEnvironment) {
        this.azureEnvironment = azureEnvironment;

        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                commonAuthority = LabConstants.COMMON_AUTHORITY;
                organizationsAuthority = LabConstants.ORGANIZATIONS_AUTHORITY;
                graphDefaultScope = LabConstants.GRAPH_DEFAULT_SCOPE;
                appProvider = new AppCredentialProvider(azureEnvironment);
                tenant = LabConstants.MICROSOFT_AUTHORITY_TENANT;
                break;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                commonAuthority = LabConstants.ARLINGTON_COMMON_AUTHORITY;
                organizationsAuthority = LabConstants.ARLINGTON_ORGANIZATIONS_AUTHORITY;
                graphDefaultScope = LabConstants.ARLINGTON_GRAPH_DEFAULT_SCOPE;
                appProvider = new AppCredentialProvider(azureEnvironment);
                tenant = LabConstants.ARLINGTON_AUTHORITY_TENANT;
                break;
            default:
                throw new UnsupportedOperationException("Azure Environment - " + azureEnvironment + " unsupported");
        }
    }

    public String commonAuthority() {
        return this.commonAuthority;
    }

    public String organizationsAuthority() {
        return this.organizationsAuthority;
    }

    public String graphDefaultScope() {
        return this.graphDefaultScope;
    }

    public String tenant() {
        return this.tenant;
    }

    public String tenantSpecificAuthority() {
        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                return LabConstants.MICROSOFT_AUTHORITY_HOST + tenant;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                return LabConstants.ARLINGTON_MICROSOFT_AUTHORITY_HOST + tenant;
            default:
                throw new UnsupportedOperationException("Azure Environment - " + azureEnvironment + " unsupported");
        }
    }
}
