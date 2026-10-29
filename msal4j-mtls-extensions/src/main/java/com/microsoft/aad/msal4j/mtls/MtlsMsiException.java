// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

/**
 * Thrown when the KeyGuard-backed managed identity mTLS binding flow fails.
 */
public class MtlsMsiException extends RuntimeException {

    public MtlsMsiException(String message) {
        super(message);
    }

    public MtlsMsiException(String message, Throwable cause) {
        super(message, cause);
    }
}
