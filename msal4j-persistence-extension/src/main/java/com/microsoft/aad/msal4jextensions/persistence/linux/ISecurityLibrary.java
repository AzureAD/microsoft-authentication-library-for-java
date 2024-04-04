// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.linux;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;

/**
 * Interface which represents API for libsecret library on Linux.
 * It is used for storing and retrieving passwords and other secrets on Linux.
 * It communicates with the "Secret Service" using D-Bus
 * https://developer.gnome.org/libsecret/0.18/
 */
public interface ISecurityLibrary extends Library {

    ISecurityLibrary library = Native.load("libsecret-1", ISecurityLibrary.class);

    /**
     * Creates a schema for saving secret, it represents a set of attributes that are stored with it
     *
     * @param name           Name of the schema
     * @param flag           The flag for the schema definition
     *                       {@link com.microsoft.aad.msal4jextensions.persistence.linux.SecretSchemaFlags}
     * @param attribute1Key  Name/Key of the attribute1 of the schema
     * @param attribute1Type Type of the attribute1 of the schema
     *                       {@link com.microsoft.aad.msal4jextensions.persistence.linux.SecretSchemaAttributeType}
     * @param attribute2Key  Name/Key of the attribute2 of the schema
     * @param attribute2Type Type of the attribute2 of the schema
     *                       {@link com.microsoft.aad.msal4jextensions.persistence.linux.SecretSchemaAttributeType}
     * @param end            Null parameter to indicate end of attributes
     * @return A schema for saving and retrieving secret
     */
    Pointer secret_schema_new(String name,
                              int flag,
                              String attribute1Key, int attribute1Type,
                              String attribute2Key, int attribute2Type,
                              Pointer end);

    /**
     * Store a password in the secret service
     *
     * @param schema          Schema for attributes
     * @param collection      A collection alias, or D-Bus object path of the collection where to store the secret
     * @param label           Label for the secret
     * @param password        The secret to save
     * @param cancellable     Optional cancellation object
     * @param error           Location to place an error on failure
     * @param attribute1Key   Key of the attribute1
     * @param attribute1Value Value of the attribute1
     * @param attribute2Key   Key of the attribute2
     * @param attribute2Value Value of the attribute2
     * @param end             Null parameter to indicate end of attributes
     * @return Whether the storage was successful or not
     */
    int secret_password_store_sync(Pointer schema,
                                   String collection,
                                   String label,
                                   String password,
                                   Pointer cancellable,
                                   Pointer[] error,
                                   String attribute1Key, String attribute1Value,
                                   String attribute2Key, String attribute2Value,
                                   Pointer end);

    /**
     * Lookup a password in the secret service
     *
     * @param schema          The schema for the attributes
     * @param cancellable     Optional cancellation object
     * @param error           Location to place an error on failure
     * @param attribute1Key   Key of the attribute1
     * @param attribute1Value Value of the attribute1
     * @param attribute2Key   Key of the attribute2
     * @param attribute2Value Value of the attribute2
     * @param end             Null parameter to indicate end of attributes
     * @return The retrieved secret
     */
    String secret_password_lookup_sync(Pointer schema,
                                       Pointer cancellable,
                                       Pointer[] error,
                                       String attribute1Key, String attribute1Value,
                                       String attribute2Key, String attribute2Value,
                                       Pointer end);

    /**
     * Remove unlocked matching passwords from the secret service
     *
     * @param schema          The schema for the attributes
     * @param cancellable     Optional cancellation object
     * @param error           Location to place an error on failure
     * @param attribute1Key   Key of the attribute1
     * @param attribute1Value Value of the attribute1
     * @param attribute2Key   Key of the attribute2
     * @param attribute2Value Value of the attribute2
     * @param end             Null parameter to indicate end of attributes
     * @return Whether the any passwords were removed
     */
    int secret_password_clear_sync(Pointer schema,
                                   Pointer cancellable,
                                   Pointer[] error,
                                   String attribute1Key, String attribute1Value,
                                   String attribute2Key, String attribute2Value,
                                   Pointer end);
}
