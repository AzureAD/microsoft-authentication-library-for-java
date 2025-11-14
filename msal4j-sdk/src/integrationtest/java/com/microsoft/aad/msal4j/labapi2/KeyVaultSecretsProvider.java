// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.labapi2;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.microsoft.aad.msal4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import reactor.core.publisher.Mono;

public class KeyVaultSecretsProvider implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(KeyVaultSecretsProvider.class);

    public static class KeyVaultInstance {
        /**
         * The KeyVault maintained by the MSID. It is recommended for use.
         */
        public static final String MSID_LAB = "https://msidlabs.vault.azure.net";

        /**
         * The KeyVault maintained by the MSAL.NET team and have full control over.
         * Should be used temporarily - secrets should be stored and managed by MSID Lab.
         */
        public static final String MSAL_TEAM = "https://id4skeyvault.vault.azure.net/";
    }

    private final SecretClient secretClient;

    /**
     * Initialize the secrets provider with the specified Key Vault address.
     *
     * Authentication using client certificate:
     *     1. Register Azure AD application of "Web app / API" type.
     *        To set up certificate based access to the application PowerShell should be used.
     *     2. Add an access policy entry to target Key Vault instance for this application.
     *
     * @param keyVaultAddress The Key Vault URI (defaults to MSID_LAB)
     */
    public KeyVaultSecretsProvider(String keyVaultAddress) {
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
            key = (PrivateKey) keystore.getKey("LabAuth.MSIDLab.com", null);
            publicCertificate = (X509Certificate) keystore.getCertificate("LabAuth.MSIDLab.com");

            log.debug("Successfully loaded client certificate from keystore");
        } catch (Exception e) {
            log.error("Error getting certificate from keystore: {}", e.getMessage(), e);
            throw new RuntimeException("Error getting certificate from keystore: " + e.getMessage());
        }
        return ClientCredentialFactory.createFromCertificate(key, publicCertificate);
    }

    @Override
    public void close() {
        // Cleanup if needed
    }
}