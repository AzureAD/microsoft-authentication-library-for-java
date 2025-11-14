// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LabUserHelper {

    private static final Logger log = LoggerFactory.getLogger(LabUserHelper.class);
    private static final LabServiceApi labService = new LabServiceApi();
    private static final ConcurrentHashMap<UserQuery, LabResponse> userCache =
            new ConcurrentHashMap<>();

    private static final KeyVaultSecretsProvider keyVaultSecretsProviderMsal;
    private static final KeyVaultSecretsProvider keyVaultSecretsProviderMsid;

    static {
        // MSAL Team vault - for configuration data
        keyVaultSecretsProviderMsal = new KeyVaultSecretsProvider(
                KeyVaultSecretsProvider.KeyVaultInstance.MSAL_TEAM);

        // MSID Lab vault - for user passwords
        keyVaultSecretsProviderMsid = new KeyVaultSecretsProvider(
                KeyVaultSecretsProvider.KeyVaultInstance.MSID_LAB);
    }

    /**
     * Get lab user data with caching support.
     *
     * @param query The UserQuery to search for
     * @return CompletableFuture containing LabResponse
     */
    public static CompletableFuture<LabResponse> getLabUserDataAsync(UserQuery query) {
        if (userCache.containsKey(query)) {
            LabResponse cached = userCache.get(query);
            log.debug("Lab cache hit: {}",
                    cached.getUser() != null ? cached.getUser().getUpn() : "N/A");
            return CompletableFuture.completedFuture(cached);
        }

        return labService.getLabResponseFromApiAsync(query)
                .thenApply(response -> {
                    if (response == null) {
                        log.error("No lab user found for query");
                        throw new LabUserNotFoundException(query, "Found no users for the given query.");
                    }

                    log.info("Lab API returned user: {}",
                            response.getUser() != null ? response.getUser().getUpn() : "N/A");

                    userCache.put(query, response);
                    return response;
                });
    }

    /**
     * Get lab data from Key Vault by secret name.
     * Uses the MSAL Team vault for configuration data.
     *
     * @param secret The Key Vault secret name
     * @return CompletableFuture containing either LabResponse or String
     */
    public static CompletableFuture<Object> getKVLabData(String secret) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Retrieving Key Vault secret: {}", secret);
                // Use MSAL Team vault for configuration
                KeyVaultSecret keyVaultSecret = keyVaultSecretsProviderMsal.getSecretByName(secret);
                String labData = keyVaultSecret.getValue();

                if (labData == null || labData.isEmpty()) {
                    log.error("Key Vault secret '{}' is empty", secret);
                    throw new LabUserNotFoundException(new UserQuery(),
                            "Found no content for secret '" + secret + "' in Key Vault.");
                }

                // Check if the value is JSON
                if (isValidJson(labData)) {
                    LabResponse response;
                    try (JsonReader jsonReader = JsonProviders.createReader(labData)) {
                        response = LabResponse.fromJson(jsonReader);
                    }

                    if (response == null) {
                        log.error("Failed to deserialize Key Vault secret '{}' to LabResponse", secret);
                        throw new LabUserNotFoundException(new UserQuery(),
                                "Failed to deserialize Key Vault secret '" + secret + "' to LabResponse.");
                    }

                    log.debug("Retrieved LabResponse from Key Vault '{}': {}", secret,
                            response.getUser() != null ? response.getUser().getUpn() :
                                    response.getApp() != null ? response.getApp().getAppId() : "Unknown");
                    return response;
                } else {
                    log.debug("Retrieved raw string from Key Vault '{}': {} characters", secret, labData.length());
                    return labData;
                }
            } catch (Exception e) {
                log.error("Failed to retrieve Key Vault secret '{}': {}", secret, e.getMessage());
                throw new RuntimeException(
                        "Failed to retrieve or parse Key Vault secret '" + secret + "'", e);
            }
        });
    }

    /**
     * Fetch user password from Key Vault.
     * Uses the MSID Lab vault (different from configuration vault).
     *
     * @param userLabName The lab name of the user (used as secret name)
     * @return The user's password
     */
    public static String fetchUserPassword(String userLabName) {
        if (userLabName == null || userLabName.trim().isEmpty()) {
            log.error("Password fetch failed: empty lab name");
            throw new IllegalArgumentException(
                    "Error: lab name is not set on user. Password retrieval failed.");
        }

        if (keyVaultSecretsProviderMsid == null || keyVaultSecretsProviderMsal == null) {
            log.error("Password fetch failed: KeyVault provider not initialized");
            throw new IllegalStateException("Error: KeyVault secrets provider is not set");
        }

        try {
            log.debug("Fetching user password from MSID Lab Key Vault for: {}", userLabName);
            //  Use MSID vault for passwords, not MSAL vault
            KeyVaultSecret keyVaultSecret = keyVaultSecretsProviderMsid.getSecretByName(userLabName);
            String password = keyVaultSecret.getValue();

            if (password != null && !password.isEmpty()) {
                log.debug("Password retrieved for user: {} ({} characters)", userLabName, password.length());
                return password;
            }

            log.error("Password empty for user: {}", userLabName);
            throw new IllegalStateException(
                    "Password secret '" + userLabName + "' found but was empty in Key Vault.");
        } catch (Exception e) {
            log.error("Password fetch failed for user {}: {}", userLabName, e.getMessage());
            throw new RuntimeException(
                    "Test setup: cannot get the user password from Key Vault secret '" +
                            userLabName + "'", e);
        }
    }

    /**
     * Merge multiple Key Vault secrets into a single LabResponse.
     * Each secret should contain a LabResponse JSON object.
     * Fields from later secrets override fields from earlier ones.
     *
     * @param secrets Array of Key Vault secret names to merge
     * @return Merged LabResponse
     */
    public static LabResponse mergeKVLabData(String... secrets) {
        if (secrets == null || secrets.length == 0) {
            throw new IllegalArgumentException(
                    "At least one secret name must be provided.");
        }

        try {
            LabResponse mergedResponse = new LabResponse();
            boolean hasValidResponse = false;

            for (String secret : secrets) {

                Object data = getKVLabData(secret).join();

                if (data instanceof LabResponse) {
                    LabResponse response = (LabResponse) data;
                    hasValidResponse = true;

                    // Merge user, app, and lab fields (later values override earlier ones)
                    if (response.getUser() != null) {
                        mergedResponse.setUser(response.getUser());
                    }
                    if (response.getApp() != null) {
                        mergedResponse.setApp(response.getApp());
                    }
                    if (response.getLab() != null) {
                        mergedResponse.setLab(response.getLab());
                    }
                }
            }

            if (!hasValidResponse) {
                log.error("Failed to merge secrets - no valid LabResponse found: {}",
                        String.join(", ", secrets));
                throw new LabUserNotFoundException(new UserQuery(),
                        "Failed to create merged LabResponse from secrets: " +
                                String.join(", ", secrets));
            }

            log.info("Merged secrets [{}]: {}", String.join(", ", secrets),
                    mergedResponse.getUser() != null ? mergedResponse.getUser().getUpn() : "N/A");

            return mergedResponse;
        } catch (Exception e) {
            log.error("Failed to merge secrets [{}]: {}", String.join(", ", secrets), e.getMessage());
            throw new RuntimeException(
                    "Failed to merge Key Vault secrets: " + String.join(", ", secrets), e);
        }
    }

    /**
     * Check if a string is valid JSON.
     */
    private static boolean isValidJson(String value) {
        try (JsonReader jsonReader = JsonProviders.createReader(value)) {
            jsonReader.nextToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static CompletableFuture<LabResponse> getDefaultUserAsync() {
        return CompletableFuture.completedFuture(
                mergeKVLabData("MSAL-User-Default-JSON", "ID4SLAB1", "MSAL-App-Default-JSON"));
    }

    public static CompletableFuture<LabResponse> getDefaultAdfsUserAsync() {
        return CompletableFuture.completedFuture(
                mergeKVLabData("MSAL-USER-FedDefault-JSON", "ID4SLAB1", "MSAL-App-Default-JSON"));
    }

    public static CompletableFuture<LabResponse> getB2CLocalAccountAsync() {
        return getLabUserDataAsync(UserQuery.b2cLocalAccountUserQuery());
    }

    public static CompletableFuture<LabResponse> getArlingtonUserAsync() {
        // Query the Lab API with Arlington-specific parameters
        return getLabUserDataAsync(UserQuery.arlingtonUserQuery());
    }

    public static CompletableFuture<LabResponse> getArlingtonADFSUserAsync() {
        // Create a modified query with federated user type
        UserQuery query = UserQuery.arlingtonUserQuery();
        query.setUserType(LabServiceParameters.UserType.FEDERATED);

       return getLabUserDataAsync(query);
    }

    /**
     * Get a default managed user for the specified Azure environment.
     * This is the primary helper method for parameterized tests that run across multiple clouds.
     *
     * @param azureEnvironment The Azure environment (e.g., AzureEnvironment.AZURE or AZURE_US_GOVERNMENT)
     * @return CompletableFuture containing LabResponse for a managed user in that environment
     */
    public static CompletableFuture<LabResponse> getDefaultUserAsync(String azureEnvironment) {
        log.debug("Getting default user for environment: {}", azureEnvironment);

        if (AzureEnvironment.AZURE.equals(azureEnvironment)) {
            return getDefaultUserAsync();
        } else if (AzureEnvironment.AZURE_US_GOVERNMENT.equals(azureEnvironment)) {
            return getArlingtonUserAsync();
        } else {
            log.error("Unsupported Azure environment: {}", azureEnvironment);
            throw new IllegalArgumentException("Unsupported Azure environment: " + azureEnvironment);
        }
    }

    /**
     * Get a default ADFS user for the specified Azure environment.
     * Currently ADFS users are environment-agnostic and come from Key Vault.
     *
     * @param azureEnvironment The Azure environment (included for consistency, currently unused)
     * @return CompletableFuture containing LabResponse for an ADFS federated user
     */
    public static CompletableFuture<LabResponse> getDefaultAdfsUserAsync(String azureEnvironment) {
        log.debug("Getting default ADFS user for environment: {}", azureEnvironment);

        if (AzureEnvironment.AZURE.equals(azureEnvironment)) {
            return getDefaultUserAsync();
        } else if (AzureEnvironment.AZURE_US_GOVERNMENT.equals(azureEnvironment)) {
            return getArlingtonADFSUserAsync();
        } else {
            log.error("Unsupported Azure environment: {}", azureEnvironment);
            throw new IllegalArgumentException("Unsupported Azure environment: " + azureEnvironment);
        }
    }
}