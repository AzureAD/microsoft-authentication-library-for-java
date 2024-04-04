// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

/**
 * Runtime Exception representing error/failure to access Cache File
 */
public class CacheFileAccessException extends RuntimeException {

    CacheFileAccessException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
