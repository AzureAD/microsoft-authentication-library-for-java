// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class Config {
    private String organizationsAuthority;
    private String graphDefaultScope;
    AppCredentialProvider appProvider;
    private String tenant;

    String azureEnvironment;

    public Config(String azureEnvironment) {
        this.azureEnvironment = azureEnvironment;

        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                organizationsAuthority = LabApiConstants.ORGANIZATIONS_AUTHORITY;
                graphDefaultScope = LabApiConstants.GRAPH_DEFAULT_SCOPE;
                appProvider = new AppCredentialProvider(azureEnvironment);
                tenant = LabApiConstants.MICROSOFT_AUTHORITY_TENANT;
                break;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                organizationsAuthority = LabApiConstants.ARLINGTON_ORGANIZATIONS_AUTHORITY;
                graphDefaultScope = LabApiConstants.ARLINGTON_GRAPH_DEFAULT_SCOPE;
                appProvider = new AppCredentialProvider(azureEnvironment);
                tenant = LabApiConstants.ARLINGTON_AUTHORITY_TENANT;
                break;
            default:
                throw new UnsupportedOperationException("Azure Environment - " + azureEnvironment + " unsupported");
        }
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
}
