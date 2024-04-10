// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.linux;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;
import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.nimbusds.jose.util.StandardCharset;
import com.sun.jna.Pointer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Implementation of CacheAccessor based on KeyRing for Linux
 */
public class KeyRingAccessor implements ICacheAccessor {

    private String cacheFilePath;

    private String keyringCollection;
    private String keyringSchemaName;
    private String keyringSecretLabel;

    private String attributeKey1;
    private String attributeValue1;

    private String attributeKey2;
    private String attributeValue2;

    private Pointer libSecretSchema;

    public KeyRingAccessor(String cacheFilePath,
                           String keyringCollection,
                           String keyringSchemaName,
                           String keyringSecretLabel,
                           String attributeKey1, String attributeValue1,
                           String attributeKey2, String attributeValue2) {

        this.cacheFilePath = cacheFilePath;
        this.keyringCollection = keyringCollection;
        this.keyringSchemaName = keyringSchemaName;
        this.keyringSecretLabel = keyringSecretLabel;
        this.attributeKey1 = attributeKey1;
        this.attributeValue1 = attributeValue1;
        this.attributeKey2 = attributeKey2;
        this.attributeValue2 = attributeValue2;
    }

    public void verify() {
        String testAttributeValue1 = "testAttr1";
        String testAttributeValue2 = "testAttr2";
        String testData = "Test Data";

        write(testData.getBytes(StandardCharsets.UTF_8), testAttributeValue1, testAttributeValue2);

        byte[] readData = read(testAttributeValue1, testAttributeValue2);

        if (readData == null || !testData.equals(new String(readData, StandardCharset.UTF_8))) {
            throw new KeyRingAccessException("An error while validating KeyRing availability");
        }

        delete(testAttributeValue1, testAttributeValue2);
    }

    private byte[] read(String attributeValue1, String attributeValue2) {
        byte[] data = null;

        Pointer[] error = new Pointer[1];

        String secret = ISecurityLibrary.library.secret_password_lookup_sync(
                getLibSecretSchema(),
                null,
                error,
                attributeKey1, attributeValue1,
                attributeKey2, attributeValue2,
                null);

        if (error[0] != Pointer.NULL) {
            GError err = new GError(error[0]);
            throw new KeyRingAccessException("An error while reading secret from keyring, " +
                    "domain:" + err.domain + " code:" + err.code + " message:" + err.message);
        } else if (secret != null && !secret.isEmpty()) {
            data = Base64.getDecoder().decode(secret);
        }

        return data;
    }

    @Override
    public byte[] read() {
        return read(attributeValue1, attributeValue2);
    }

    private void write(byte[] data, String attributeValue1, String attributeValue2) {
        Pointer[] error = new Pointer[1];

        ISecurityLibrary.library.secret_password_store_sync(
                getLibSecretSchema(),
                keyringCollection,
                keyringSecretLabel,
                Base64.getEncoder().encodeToString(data),
                null,
                error,
                attributeKey1, attributeValue1,
                attributeKey2, attributeValue2,
                null);

        if (error[0] != Pointer.NULL) {
            GError err = new GError(error[0]);

            throw new KeyRingAccessException("An error while saving secret to keyring, " +
                    "domain:" + err.domain + " code:" + err.code + " message:" + err.message);
        }
        new CacheFileAccessor(cacheFilePath).updateCacheFileLastModifiedTime();
    }

    @Override
    public void write(byte[] data) {
        write(data, attributeValue1, attributeValue2);
    }

    private void delete(String attributeValue1, String attributeValue2) {
        Pointer[] error = new Pointer[1];

        ISecurityLibrary.library.secret_password_clear_sync(
                getLibSecretSchema(),
                null,
                error,
                attributeKey1, attributeValue1,
                attributeKey2, attributeValue2,
                null);

        if (error[0] != Pointer.NULL) {
            GError err = new GError(error[0]);

            throw new KeyRingAccessException("An error while deleting secret from keyring, " +
                    "domain:" + err.domain + " code:" + err.code + " message:" + err.message);
        }
        new CacheFileAccessor(cacheFilePath).updateCacheFileLastModifiedTime();
    }

    @Override
    public void delete() {
        delete(attributeValue1, attributeValue2);
    }

    private Pointer getLibSecretSchema() {
        if (libSecretSchema == Pointer.NULL) {
            libSecretSchema = ISecurityLibrary.library.secret_schema_new(
                    keyringSchemaName,
                    SecretSchemaFlags.SECRET_SCHEMA_NONE,
                    attributeKey1,
                    SecretSchemaAttributeType.SECRET_SCHEMA_ATTRIBUTE_STRING,
                    attributeKey2,
                    SecretSchemaAttributeType.SECRET_SCHEMA_ATTRIBUTE_STRING,
                    null);

            if (libSecretSchema == Pointer.NULL) {
                throw new KeyRingAccessException
                        ("Failed to create libSecret schema " + keyringSchemaName);
            }
        }
        return libSecretSchema;
    }
}
