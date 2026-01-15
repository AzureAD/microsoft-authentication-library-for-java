// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

/**
 * Helper class to provide access to different Key Vault secrets providers.
 */
public class KeyVaultRegistry {
    private static final KeyVaultSecretsProvider MSID_LAB_PROVIDER =
            new KeyVaultSecretsProvider(KeyVaultSecretsProvider.KeyVaultInstance.MSID_LAB);

    private static final KeyVaultSecretsProvider MSAL_TEAM_PROVIDER =
            new KeyVaultSecretsProvider(KeyVaultSecretsProvider.KeyVaultInstance.MSAL_TEAM);

    /**
     * This Key Vault is primarily used for frequently rotated credentials.
     */
    public static KeyVaultSecretsProvider getMsidLabProvider() {
        return MSID_LAB_PROVIDER;
    }

    /**
     * This Key Vault is primarily used for user/app/etc. configuration and other long-lived info.
     */
    public static KeyVaultSecretsProvider getMsalTeamProvider() {
        return MSAL_TEAM_PROVIDER;
    }

    private KeyVaultRegistry() {
        // Prevent instantiation
    }
}

