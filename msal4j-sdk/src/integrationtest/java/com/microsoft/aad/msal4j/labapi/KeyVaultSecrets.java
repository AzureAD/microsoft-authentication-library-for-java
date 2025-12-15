// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

/**
 * Constants for Key Vault secret names used in integration tests.
 */
public final class KeyVaultSecrets {

    private KeyVaultSecrets() {
        // Prevent instantiation
    }

    // User Configuration Secrets
    public static final String USER_PUBLIC_CLOUD = "User-PublicCloud-Config";
    public static final String USER_FED_DEFAULT = "User-Federated-Config";

    // TODO: Consolidate with others or following naming convention in key vault
    public static final String USER_B2C = "MSAL-USER-B2C-JSON";
    public static final String USER_ARLINGTON = "MSAL-USER-Arlington-JSON";
    public static final String USER_CIAM = "MSAL-USER-CIAM-JSON";

    // App Configuration Secrets
    public static final String APP_PCACLIENT = "App-PCAClient-Config";
    public static final String APP_WEBAPI = "App-WebAPI-Config";
    public static final String APP_S2S = "App-S2S-Config";

    // TODO: Consolidate with others or following naming convention in key vault
    public static final String APP_B2C = "MSAL-App-B2C-JSON";
    public static final String APP_ARLINGTON = "MSAL-App-Arlington-JSON";
    public static final String APP_CIAM = "MSAL-App-CIAM-JSON";

    // Lab Configuration Secrets
    public static final String LAB_ID4SLAB1 = "ID4SLAB1";
    public static final String LAB_ARLMSIDLAB1 = "ARLMSIDLAB1";
}

