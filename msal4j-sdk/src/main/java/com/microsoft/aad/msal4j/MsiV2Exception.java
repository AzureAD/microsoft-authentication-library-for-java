// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Exception type thrown when an error occurs during MSI v2 (mTLS Proof-of-Possession with
 * KeyGuard attestation) token acquisition.
 * <p>
 * MSI v2 errors are not recoverable by falling back to MSI v1. When MSI v2 is explicitly
 * requested (both {@code mtlsProofOfPossession=true} and {@code withAttestationSupport=true}),
 * any failure in the MSI v2 flow will result in this exception rather than a silent fallback.
 */
public class MsiV2Exception extends MsalException {

    /**
     * Initializes a new instance of the exception class with a message and error code.
     *
     * @param message   the error message that explains the reason for the exception
     * @param errorCode a simplified error code for references in documentation
     */
    public MsiV2Exception(final String message, final String errorCode) {
        super(message, errorCode);
    }

    /**
     * Initializes a new instance of the exception class with a message, error code, and cause.
     *
     * @param message   the error message that explains the reason for the exception
     * @param errorCode a simplified error code for references in documentation
     * @param cause     the inner exception that is the cause of the current exception
     */
    public MsiV2Exception(final String message, final String errorCode, final Throwable cause) {
        super(message, errorCode);
        initCause(cause);
    }
}
