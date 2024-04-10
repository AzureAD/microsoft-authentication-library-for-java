// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence.mac;

/**
 * Runtime Exception representing error/failure to access KeyChain on Mac
 */
public class KeyChainAccessException extends RuntimeException {

    KeyChainAccessException(String message) {
        super(message);
    }
}
