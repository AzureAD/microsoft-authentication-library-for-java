// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to provide configuration needed by integration tests, such as Azure user and app info.
 *
 * The returned LabResponse objects merge user, app, and lab configuration that represent a specific test scenario.
 */
public class LabResponseHelper {

    // Static caches for configuration instances
    private static final ConcurrentHashMap<String, AppConfig> appConfigCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, UserConfig> userConfigCache = new ConcurrentHashMap<>();

    /**
     * Retrieves an AppConfig from Key Vault by secret name.
     * Results are cached to avoid redundant Key Vault calls.
     *
     * @param appConfigName The Key Vault secret name for the app configuration (use KeyVaultSecrets constants)
     * @return AppConfig instance
     */
    public static AppConfig getAppConfig(String appConfigName) {
        return appConfigCache.computeIfAbsent(appConfigName, key -> {
            Object data = KeyVaultRegistry.getMsalTeamProvider().getLabData(key);
            if (data instanceof LabResponse) {
                return ((LabResponse) data).getApp();
            }
            throw new RuntimeException("Expected LabResponse with AppConfig for secret: " + key);
        });
    }

    /**
     * Retrieves a UserConfig from Key Vault by secret name.
     * Results are cached to avoid redundant Key Vault calls.
     *
     * @param userConfigName The Key Vault secret name for the user configuration (use KeyVaultSecrets constants)
     * @return UserConfig instance
     */
    public static UserConfig getUserConfig(String userConfigName) {
        return userConfigCache.computeIfAbsent(userConfigName, key -> {
            Object data = KeyVaultRegistry.getMsalTeamProvider().getLabData(key);
            if (data instanceof LabResponse) {
                return ((LabResponse) data).getUser();
            }
            throw new RuntimeException("Expected LabResponse with UserConfig for secret: " + key);
        });
    }
}