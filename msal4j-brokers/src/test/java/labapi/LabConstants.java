// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package labapi;

public class LabConstants {
    public final static String KEYVAULT_DEFAULT_SCOPE = "https://vault.azure.net/.default";
    public final static String MSIDLAB_DEFAULT_SCOPE = "https://msidlab.com/.default";
    public final static String MSIDLAB_VAULT_URL = "https://msidlabs.vault.azure.net/";

    public final static String MICROSOFT_AUTHORITY =
            "https://login.microsoftonline.com/microsoft.onmicrosoft.com";

    public final static String LAB_USER_ENDPOINT = "https://msidlab.com/api/user";
    public final static String LAB_USER_SECRET_ENDPOINT = "https://msidlab.com/api/LabSecret";

    public final static String APP_ID_KEY_VAULT_SECRET =
            "https://msidlabs.vault.azure.net/secrets/LabVaultAppID";
    public final static String APP_PASSWORD_KEY_VAULT_SECRET =
            "https://msidlabs.vault.azure.net/secrets/LabVaultAppSecret";

    public final static String AZURE_ENVIRONMENT = "azurecloud";
    public final static String FEDERATION_PROVIDER_NONE = "none";
}
