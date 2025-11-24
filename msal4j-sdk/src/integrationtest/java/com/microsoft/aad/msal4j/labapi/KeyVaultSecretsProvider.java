// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.microsoft.aad.msal4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;

public class KeyVaultSecretsProvider implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KeyVaultSecretsProvider.class);

    /**
     * The certificate alias used for authentication with Key Vault.
     */
    public static final String CERTIFICATE_ALIAS = "LabAuth.MSIDLab.com";

    static class KeyVaultInstance {
        /**
         * The KeyVault maintained by the MSID. It is recommended for use.
         */
        static final String MSID_LAB = "https://msidlabs.vault.azure.net";

        /**
         * The KeyVault maintained by the MSAL.NET team and have full control over.
         * Should be used temporarily - secrets should be stored and managed by MSID Lab.
         */
        static final String MSAL_TEAM = "https://id4skeyvault.vault.azure.net/";
    }

    private final SecretClient secretClient;

    /**
     * Initialize the secrets provider with the specified Key Vault address.
     * <p>
     * Authentication using client certificate:
     * 1. Register Azure AD application of "Web app / API" type.
     * To set up certificate based access to the application PowerShell should be used.
     * 2. Add an access policy entry to target Key Vault instance for this application.
     *
     * @param keyVaultAddress The Key Vault URI (defaults to MSID_LAB)
     */
    KeyVaultSecretsProvider(String keyVaultAddress) {
        String vaultUrl = keyVaultAddress != null ? keyVaultAddress : KeyVaultInstance.MSID_LAB;
        log.debug("Initializing KeyVault secrets provider for: {}", vaultUrl);

        TokenCredential credentials = getKeyVaultCredential();

        this.secretClient = new SecretClientBuilder()
                .vaultUrl(vaultUrl)
                .credential(credentials)
                .buildClient();

        log.debug("KeyVault secrets provider initialized successfully");
    }

    public KeyVaultSecretsProvider() {
        this(KeyVaultInstance.MSID_LAB);
    }

    /**
     * Get a secret by name from Key Vault.
     *
     * @param secretName The name of the secret
     * @return The KeyVaultSecret object
     */
    public KeyVaultSecret getSecretByName(String secretName) {
        log.debug("Retrieving secret from Key Vault: {}", secretName);
        try {
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            log.debug("Successfully retrieved secret: {}", secretName);
            return secret;
        } catch (Exception e) {
            log.error("Failed to retrieve secret '{}': {}", secretName, e.getMessage());
            throw e;
        }
    }

    /**
     * Get credentials for accessing Key Vault.
     * Uses LabAuthenticationHelper to obtain an access token.
     *
     * @return TokenCredential for Key Vault access
     */
    private TokenCredential getKeyVaultCredential() {
        return tokenRequestContext -> Mono.defer(() -> Mono.just(requestAccessTokenForAutomation()));
    }

    private AccessToken requestAccessTokenForAutomation() {
        IAuthenticationResult result;
        try {
            log.debug("Acquiring access token for Key Vault");
            ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                            TestConstants.MSIDLAB_CLIENT_ID,
                            getClientCredentialFromKeyStore())
                    .authority(TestConstants.MICROSOFT_AUTHORITY)
                    .sendX5c(true)
                    .build();

            result = cca.acquireToken(ClientCredentialParameters
                            .builder(Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE))
                            .build())
                    .get();

            log.debug("Successfully acquired Key Vault access token");
        } catch (Exception e) {
            log.error("Error acquiring token from Azure AD: {}", e.getMessage(), e);
            throw new RuntimeException("Error acquiring token from Azure AD: " + e.getMessage());
        }

        if (result != null) {
            return new AccessToken(
                    result.accessToken(),
                    OffsetDateTime.ofInstant(result.expiresOnDate().toInstant(), ZoneOffset.UTC));
        } else {
            log.error("Authentication result is null");
            throw new NullPointerException("Authentication result is null");
        }
    }

    IClientCredential getClientCredentialFromKeyStore() {
        PrivateKey key;
        X509Certificate publicCertificate;
        try {
            log.debug("Loading client certificate from keystore");
            String os = System.getProperty("os.name");
            KeyStore keystore;
            if (os.toLowerCase().contains("windows")) {
                log.debug("Using Windows-MY keystore");
                keystore = KeyStore.getInstance("Windows-MY", "SunMSCAPI");
            } else {
                log.debug("Using KeychainStore keystore");
                keystore = KeyStore.getInstance("KeychainStore");
            }

            keystore.load(null, null);
            key = (PrivateKey) keystore.getKey(CERTIFICATE_ALIAS, null);
            publicCertificate = (X509Certificate) keystore.getCertificate(CERTIFICATE_ALIAS);

            log.debug("Successfully loaded client certificate from keystore");
        } catch (Exception e) {
            log.error("Error getting certificate from keystore: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting certificate from keystore: " + e.getMessage());
        }
        return ClientCredentialFactory.createFromCertificate(key, publicCertificate);
    }

    /**
     * Get lab data from Key Vault by secret name.
     * Automatically deserializes JSON content to LabResponse if valid JSON.
     *
     * @param secretName The Key Vault secret name
     * @return Either LabResponse (if JSON) or String (if raw text)
     */
    public Object getLabData(String secretName) {
        try {
            log.info("Retrieving Key Vault secret: {}", secretName);
            KeyVaultSecret keyVaultSecret = getSecretByName(secretName);
            String labData = keyVaultSecret.getValue();

            if (labData == null || labData.isEmpty()) {
                log.error("Key Vault secret '{}' is empty", secretName);
                throw new LabUserNotFoundException(new UserQueryHelper(),
                        "Found no content for secret '" + secretName + "' in Key Vault.");
            }

            // Check if the value is JSON
            if (isValidJson(labData)) {
                LabResponse response;
                try (JsonReader jsonReader = JsonProviders.createReader(labData)) {
                    response = LabResponse.fromJson(jsonReader);
                }

                if (response == null) {
                    log.error("Failed to deserialize Key Vault secret '{}' to LabResponse", secretName);
                    throw new LabUserNotFoundException(new UserQueryHelper(),
                            "Failed to deserialize Key Vault secret '" + secretName + "' to LabResponse.");
                }

                log.debug("Retrieved LabResponse from Key Vault '{}': {}", secretName,
                        response.getUser() != null ? response.getUser().getUpn() :
                                response.getApp() != null ? response.getApp().getAppId() : "Unknown");
                return response;
            } else {
                log.debug("Retrieved raw string from Key Vault '{}': {} characters", secretName, labData.length());
                return labData;
            }
        } catch (Exception e) {
            log.error("Failed to retrieve Key Vault secret '{}': {}", secretName, e.getMessage());
            throw new RuntimeException(
                    "Failed to retrieve or parse Key Vault secret '" + secretName + "'", e);
        }
    }

    /**
     * Merge multiple Key Vault secrets into a single LabResponse.
     * Each secret should contain a LabResponse JSON object.
     * Fields from later secrets override fields from earlier ones.
     *
     * @param secretNames Array of Key Vault secret names to merge
     * @return Merged LabResponse
     */
    public LabResponse mergeLabResponses(String... secretNames) {
        if (secretNames == null || secretNames.length == 0) {
            throw new IllegalArgumentException(
                    "At least one secret name must be provided.");
        }

        try {
            LabResponse mergedResponse = new LabResponse();
            boolean hasValidResponse = false;

            for (String secretName : secretNames) {
                Object data = getLabData(secretName);

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
                        String.join(", ", secretNames));
                throw new LabUserNotFoundException(new UserQueryHelper(),
                        "Failed to create merged LabResponse from secrets: " +
                                String.join(", ", secretNames));
            }

            log.info("Merged secrets [{}]: {}", String.join(", ", secretNames),
                    mergedResponse.getUser() != null ? mergedResponse.getUser().getUpn() : "N/A");

            return mergedResponse;
        } catch (Exception e) {
            log.error("Failed to merge secrets [{}]: {}", String.join(", ", secretNames), e.getMessage());
            throw new RuntimeException(
                    "Failed to merge Key Vault secrets: " + String.join(", ", secretNames), e);
        }
    }

    /**
     * Fetch user password from Key Vault.
     * Note: This should only be called on instances configured for the MSID Lab vault.
     *
     * @param userLabName The lab name of the user (used as secret name)
     * @return The user's password
     */
    public String getUserPassword(String userLabName) {
        if (userLabName == null || userLabName.trim().isEmpty()) {
            log.error("Password fetch failed: empty lab name");
            throw new IllegalArgumentException(
                    "Error: lab name is not set on user. Password retrieval failed.");
        }

        try {
            log.debug("Fetching user password from Key Vault for: {}", userLabName);
            KeyVaultSecret keyVaultSecret = getSecretByName(userLabName);
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

    @Override
    public void close() {
    }
}