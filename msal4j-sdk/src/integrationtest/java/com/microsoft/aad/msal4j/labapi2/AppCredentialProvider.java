// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

public class AppCredentialProvider {

    private String clientId;
    private String oboClientId;
    private String oboAppPassword;
    private String oboAppIdURI;

    public AppCredentialProvider(String azureEnvironment) {
        KeyVaultSecretsProvider keyVaultSecretsProvider = new KeyVaultSecretsProvider();

        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                clientId = "c0485386-1e9a-4663-bc96-7ab30656de7f";
                oboClientId = "f4aa5217-e87c-42b2-82af-5624dd14ee72";
                oboAppIdURI = "api://f4aa5217-e87c-42b2-82af-5624dd14ee72";
                oboAppPassword = keyVaultSecretsProvider.getSecretByName("TodoListServiceV2-OBO").getValue();
                break;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                clientId = LabConstants.ARLINGTON_APP_ID;
                oboClientId = LabConstants.ARLINGTON_OBO_APP_ID;
                oboAppIdURI = "https://arlmsidlab1.us/IDLABS_APP_Confidential_Client";
                // TODO: Arlington OBO password needs to be retrieved from Key Vault or configured
                oboAppPassword = null;
                break;
            default:
                throw new UnsupportedOperationException("Azure Environment - " + azureEnvironment + " unsupported");
        }
    }

    public String getAppId() {
        return clientId;
    }

    public String getOboAppId() {
        return oboClientId;
    }

    public String getOboAppPassword() {
        return oboAppPassword;
    }

    public String getOboAppIdURI() {
        return oboAppIdURI;
    }
}
