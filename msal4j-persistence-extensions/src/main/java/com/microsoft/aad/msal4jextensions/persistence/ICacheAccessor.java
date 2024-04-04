// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

/**
 * Interface for cache data access operations.
 */
public interface ICacheAccessor {

    /**
     * Reads cache data
     *
     * @return Cache data
     */
    byte[] read();

    /**
     * Writes cache data
     *
     * @param data cache data
     */
    void write(byte[] data);

    /**
     * Deletes the cache
     */
    void delete();
}
