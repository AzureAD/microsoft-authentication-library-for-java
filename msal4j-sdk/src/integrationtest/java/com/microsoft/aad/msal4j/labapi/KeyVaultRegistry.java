// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

public class KeyVaultRegistry {
    private static final KeyVaultSecretsProvider MSID_LAB_PROVIDER =
            new KeyVaultSecretsProvider(KeyVaultSecretsProvider.KeyVaultInstance.MSID_LAB);

    private static final KeyVaultSecretsProvider MSAL_TEAM_PROVIDER =
            new KeyVaultSecretsProvider(KeyVaultSecretsProvider.KeyVaultInstance.MSAL_TEAM);

    public static KeyVaultSecretsProvider getMsidLabProvider() {
        return MSID_LAB_PROVIDER;
    }

    public static KeyVaultSecretsProvider getMsalTeamProvider() {
        return MSAL_TEAM_PROVIDER;
    }

    private KeyVaultRegistry() {
        // Prevent instantiation
    }
}

