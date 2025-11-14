// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class AppCredentialProvider {

    private String clientId;

    public AppCredentialProvider(String azureEnvironment) {

        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                clientId = "c0485386-1e9a-4663-bc96-7ab30656de7f";

                break;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                clientId = LabApiConstants.ARLINGTON_APP_ID;
                break;
            default:
                throw new UnsupportedOperationException("Azure Environment - " + azureEnvironment + " unsupported");
        }
    }

    public String getAppId() {
        return clientId;
    }
}
