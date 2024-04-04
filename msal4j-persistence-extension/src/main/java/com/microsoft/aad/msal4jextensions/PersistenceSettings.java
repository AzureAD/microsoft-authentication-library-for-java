// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import java.nio.file.Path;

/**
 * An immutable class containing persistence settings for MSAL caches in various platforms.
 */
public class PersistenceSettings {

    private String cacheFileName;
    private Path cacheDirectoryPath;

    private String keychainService;
    private String keychainAccount;

    private String keyringCollection;
    private String keyringSchemaName;
    private String keyringSecretLabel;
    private String keyringAttribute1Key;
    private String keyringAttribute1Value;
    private String keyringAttribute2Key;
    private String keyringAttribute2Value;

    private boolean linuxUseUnprotectedFileAsCacheStorage;

    private int lockRetryDelayMilliseconds;
    private int lockRetryNumber;

    private PersistenceSettings(String cacheFileName,
                                Path cacheDirectoryPath,
                                String keychainService,
                                String keychainAccount,
                                String keyringCollection,
                                String keyringSchemaName,
                                String keyringSecretLabel,
                                String keyringAttribute1Key,
                                String keyringAttribute1Value,
                                String keyringAttribute2Key,
                                String keyringAttribute2Value,
                                boolean linuxUseUnprotectedFileAsCacheStorage,
                                int lockRetryDelayMilliseconds,
                                int lockRetryNumber) {

        this.cacheFileName = cacheFileName;
        this.cacheDirectoryPath = cacheDirectoryPath;
        this.keychainService = keychainService;
        this.keychainAccount = keychainAccount;
        this.keyringCollection = keyringCollection;
        this.keyringSchemaName = keyringSchemaName;
        this.keyringSecretLabel = keyringSecretLabel;
        this.keyringAttribute1Key = keyringAttribute1Key;
        this.keyringAttribute1Value = keyringAttribute1Value;
        this.keyringAttribute2Key = keyringAttribute2Key;
        this.keyringAttribute2Value = keyringAttribute2Value;
        this.linuxUseUnprotectedFileAsCacheStorage = linuxUseUnprotectedFileAsCacheStorage;
        this.lockRetryDelayMilliseconds = lockRetryDelayMilliseconds;
        this.lockRetryNumber = lockRetryNumber;
    }

    /**
     * @return The name of the cache file.
     */
    public String getCacheFileName() {
        return cacheFileName;
    }

    /**
     * @return The path of the directory containing the cache file.
     */
    public Path getCacheDirectoryPath() {
        return cacheDirectoryPath;
    }

    /**
     * @return The mac keychain service.
     */
    public String getKeychainService() {
        return keychainService;
    }

    /**
     * @return The mac keychain account.
     */
    public String getKeychainAccount() {
        return keychainAccount;
    }

    /**
     * @return The linux keyring collection.
     */
    public String getKeyringCollection() {
        return keyringCollection;
    }

    /**
     * @return The linux keyring schema.
     */
    public String getKeyringSchemaName() {
        return keyringSchemaName;
    }

    /**
     * @return The linux keyring secret label.
     */
    public String getKeyringSecretLabel() {
        return keyringSecretLabel;
    }

    /**
     * @return Linux keyring additional attribute1 key.
     */
    public String getKeyringAttribute1Key() {
        return keyringAttribute1Key;
    }

    /**
     * @return Linux keyring additional attribute1 value.
     */
    public String getKeyringAttribute1Value() {
        return keyringAttribute1Value;
    }

    /**
     * @return Linux keyring additional2 attribute2 key.
     */
    public String getKeyringAttribute2Key() {
        return keyringAttribute2Key;
    }

    /**
     * @return Linux keyring additional2 attribute2 value.
     */
    public String getKeyringAttribute2Value() {
        return keyringAttribute2Value;
    }

    /**
     * @return is UNPROTECTED FILE wil be used as storage on Linux
     */
    public boolean isOnLinuxUseUnprotectedFileAsCacheStorage() {
        return linuxUseUnprotectedFileAsCacheStorage;
    }

    /**
     * @return Lock retry delay in milliseconds.
     */
    public int getLockRetryDelayMilliseconds() {
        return lockRetryDelayMilliseconds;
    }

    /**
     * @return Lock retry number.
     */
    public int getLockRetryNumber() {
        return lockRetryNumber;
    }

    private static void validateArgument(String parameter, String value) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(parameter + " null or Empty");
        }
    }

    private static void validateNotNull(String parameter, Path path) {
        if (path == null) {
            throw new IllegalArgumentException(parameter + " can not be null");
        }
    }

    /**
     * Constructs an instance of builder associated with provided cache file.
     *
     * @param cacheFileName      The name of the cache file to use when reading/writing storage.
     * @param cacheDirectoryPath The path of the directory containing the cache file.
     * @return The augmented builder
     */
    public static Builder builder(String cacheFileName, Path cacheDirectoryPath) {
        return new Builder(cacheFileName, cacheDirectoryPath);
    }

    /**
     * An builder for {@link com.microsoft.aad.msal4jextensions.PersistenceSettings} objects.
     */
    public static class Builder {

        private String cacheFileName;
        private Path cacheDirectoryPath;

        private String keychainService;
        private String keychainAccount;

        private String keyringCollection;
        private String keyringSchemaName;
        private String keyringSecretLabel;
        private String keyringAttributeKey1;
        private String keyringAttributeValue1;
        private String keyringAttributeKey2;
        private String keyringAttributeValue2;

        private boolean linuxUseUnprotectedFileAsCacheStorage = false;

        private int lockRetryDelayMilliseconds = 100;
        private int lockRetryNumber = 60;

        private Builder(String cacheFileName, Path cacheDirectoryPath) {
            validateArgument("cacheFileName", cacheFileName);
            validateNotNull("cacheDirectoryPath", cacheDirectoryPath);

            this.cacheFileName = cacheFileName;
            this.cacheDirectoryPath = cacheDirectoryPath;
        }

        /**
         * Augments this builder with mac keychain settings.
         *
         * @param service The mac keychain service.
         * @param account The mac keychain account
         * @return The augmented builder
         */
        public Builder setMacKeychain(String service, String account) {
            validateArgument("service", service);
            validateArgument("account", account);

            this.keychainAccount = account;
            this.keychainService = service;

            return this;
        }

        /**
         * Augments this builder with Linux KeyRing settings.
         *
         * @param collection      A collection aggregates multiple schema.
         *                        KeyRing defines 2 collections - "default' is a persisted schema
         *                        and "session" is an in-memory schema that is destroyed on logout.
         * @param schemaName      Schema name is a logical container of secrets, similar to a namespace.
         * @param secretLabel     A user readable label for the secret.
         * @param attributeKey1   A key for additional attribute1, that will be used to decorate the secret.
         * @param attributeValue1 A value for additional attribute1, that will be used to decorate the secret.
         * @param attributeKey2   A key for additional attribute2, that will be used to decorate the secret.
         * @param attributeValue2 A value for additional attribute2, that will be used to decorate the secret.
         * @return The augmented builder.
         */
        public Builder setLinuxKeyring(String collection,
                                       String schemaName,
                                       String secretLabel,
                                       String attributeKey1,
                                       String attributeValue1,
                                       String attributeKey2,
                                       String attributeValue2) {

            validateArgument("schemaName", schemaName);

            keyringCollection = collection;
            keyringSchemaName = schemaName;
            keyringSecretLabel = secretLabel;
            keyringAttributeKey1 = attributeKey1;
            keyringAttributeValue1 = attributeValue1;
            keyringAttributeKey2 = attributeKey2;
            keyringAttributeValue2 = attributeValue2;

            return this;
        }

        /**
         * Augments this builder with linux persistence settings to
         * use unprotected file as Cache storage.
         * KEY RING as linux storage SHOULD BE USED normally.
         * But if Key Ring is not available, Cache file can be used,
         * BUT it is RESPONSIBILITY OF USER TO PROTECT CACHE FILE,
         * That should be clearly articulated and explained to end user.
         *
         * @param useUnprotectedFileAsCacheStorage boolean value
         * @return The augmented builder.
         */
        public Builder setLinuxUseUnprotectedFileAsCacheStorage(boolean useUnprotectedFileAsCacheStorage) {
            linuxUseUnprotectedFileAsCacheStorage = useUnprotectedFileAsCacheStorage;

            return this;
        }

        /**
         * Augments this builder with lock retry settings.
         *
         * @param delayMilliseconds Delay between retries in ms, must be 1 or more.
         *                          Default value is 100 ms.
         * @param retryNumber       Number of retries, must be 1 or more.
         *                          Default value is 60.
         * @return The augmented builder.
         */
        public Builder setLockRetry(int delayMilliseconds, int retryNumber) {

            if (lockRetryDelayMilliseconds < 1) {
                throw new IllegalArgumentException("delayMilliseconds value should be more than 0");
            }
            if (lockRetryNumber < 1) {
                throw new IllegalArgumentException("retryNumber value should be more than 0");
            }

            lockRetryDelayMilliseconds = delayMilliseconds;
            lockRetryNumber = retryNumber;

            return this;
        }

        /**
         * Construct an immutable instance of {@link com.microsoft.aad.msal4jextensions.PersistenceSettings}.
         *
         * @return An immutable instance of {@link com.microsoft.aad.msal4jextensions.PersistenceSettings}.
         */
        public PersistenceSettings build() {
            if (keyringSchemaName != null && linuxUseUnprotectedFileAsCacheStorage) {
                throw new IllegalArgumentException(
                        "Only one type of persistence can be used on Linux - KeyRing or Unprotected file");
            }

            return new PersistenceSettings(
                    cacheFileName,
                    cacheDirectoryPath,
                    keychainService,
                    keychainAccount,
                    keyringCollection,
                    keyringSchemaName,
                    keyringSecretLabel,
                    keyringAttributeKey1,
                    keyringAttributeValue1,
                    keyringAttributeKey2,
                    keyringAttributeValue2,
                    linuxUseUnprotectedFileAsCacheStorage,
                    lockRetryDelayMilliseconds,
                    lockRetryNumber);
        }
    }
}
