// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

public class AppCredentialProvider {
    private KeyVaultSecretsProvider keyVaultSecretsProvider;

    private String labVaultClientId;
    private String labVaultPassword;

    private String clientId;

    private String oboClientId;
    private String oboAppIdURI;
    private String secret;

    public AppCredentialProvider(String azureEnvironment) {
        keyVaultSecretsProvider = new KeyVaultSecretsProvider();

        labVaultClientId = keyVaultSecretsProvider.getSecret(LabConstants.APP_ID_KEY_VAULT_SECRET_KEY);
        labVaultPassword = keyVaultSecretsProvider.getSecret(LabConstants.APP_PASSWORD_KEY_VAULT_SECRET_KEY);

        switch (azureEnvironment) {
            case AzureEnvironment.AZURE:
                clientId = "c0485386-1e9a-4663-bc96-7ab30656de7f";

                oboClientId = "f4aa5217-e87c-42b2-82af-5624dd14ee72";
                oboAppIdURI = "api://f4aa5217-e87c-42b2-82af-5624dd14ee72";
                secret = keyVaultSecretsProvider.getSecret(LabConstants.OBO_APP_PASSWORD_KEY);
                break;
            case AzureEnvironment.AZURE_US_GOVERNMENT:
                clientId = LabConstants.ARLINGTON_APP_ID;

                oboClientId = LabConstants.ARLINGTON_OBO_APP_ID;
                oboAppIdURI = "https://arlmsidlab1.us/IDLABS_APP_Confidential_Client";

                secret = keyVaultSecretsProvider.getSecret(LabConstants.ARLINGTON_SECRET_KEY);
                break;
            case AzureEnvironment.CIAM:
                secret = keyVaultSecretsProvider.getSecret(LabConstants.CIAM_SECRET_KEY);

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

    public String getOboAppIdURI() {
        return oboAppIdURI;
    }

    public String getSecret() {
        return secret;
    }

    public String getLabVaultAppId() {
        return labVaultClientId;
    }

    public String getLabVaultPassword() {
        return labVaultPassword;
    }
}
