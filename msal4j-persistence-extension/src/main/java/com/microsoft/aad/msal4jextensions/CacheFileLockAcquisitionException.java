// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

/**
 * Exception represents failure/error of acquiring cross process cacheFile lock
 * */
public class CacheFileLockAcquisitionException extends RuntimeException {
    /**
     * Constructor
     * @param message Exception details
     */
    public CacheFileLockAcquisitionException(String message) {
        super(message);
    }
}
