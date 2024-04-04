// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.linux;

/**
 * Runtime Exception representing error/failure to access KeyRing on Linux
 */
public class KeyRingAccessException extends RuntimeException {

    KeyRingAccessException(String message) {
        super(message);
    }

    KeyRingAccessException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
