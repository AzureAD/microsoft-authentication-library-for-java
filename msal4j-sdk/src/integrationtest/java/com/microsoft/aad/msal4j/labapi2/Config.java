// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class Config {
    private String commonAuthority;
    private String organizationsAuthority;
    private String graphDefaultScope;
    private AppCredentialProvider appProvider;
    private String tenant;

    String azureEnvironment;

    public Config() {
        this.azureEnvironment = "azurecloud";
        commonAuthority = LabConstants.COMMON_AUTHORITY;
        organizationsAuthority = LabConstants.ORGANIZATIONS_AUTHORITY;
        graphDefaultScope = LabConstants.GRAPH_DEFAULT_SCOPE;
        appProvider = new AppCredentialProvider();
        tenant = LabConstants.MICROSOFT_AUTHORITY_TENANT;

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
        return LabConstants.MICROSOFT_AUTHORITY_HOST + tenant;
    }

    public AppCredentialProvider appProvider() {
        return appProvider;
    }
}
