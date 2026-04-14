// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

/**
 * Thrown when the mTLS Managed Identity subprocess ({@code MsalMtlsMsiHelper.exe}) fails or
 * cannot be located.
 */
public class MtlsMsiException extends Exception {

    public MtlsMsiException(String message) {
        super(message);
    }

    public MtlsMsiException(String message, Throwable cause) {
        super(message, cause);
    }
}
