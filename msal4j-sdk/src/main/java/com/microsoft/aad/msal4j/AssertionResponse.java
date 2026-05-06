// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.security.cert.X509Certificate;

/**
 * Container returned from context-aware assertion provider callbacks.
 * Allows the callback to supply both the client assertion JWT and an optional
 * token-binding certificate for mutual-TLS Proof-of-Possession (mTLS PoP) scenarios.
 *
 * <p>When a {@link #tokenBindingCertificate()} is provided, MSAL sets the
 * {@code client_assertion_type} to {@code urn:ietf:params:oauth:client-assertion-type:jwt-pop}
 * instead of the default {@code jwt-bearer}.</p>
 *
 * @see ClientCredentialFactory#createFromCallback(java.util.function.Function)
 * @see AssertionRequestOptions
 */
public class AssertionResponse {

    private final String assertion;
    private final X509Certificate tokenBindingCertificate;

    /**
     * Creates an AssertionResponse with just an assertion string (no token binding certificate).
     *
     * @param assertion the JWT assertion string
     */
    public AssertionResponse(String assertion) {
        this(assertion, null);
    }

    /**
     * Creates an AssertionResponse with an assertion string and an optional token-binding certificate.
     *
     * @param assertion               the JWT assertion string
     * @param tokenBindingCertificate optional certificate for mTLS PoP binding, or null for standard jwt-bearer
     */
    public AssertionResponse(String assertion, X509Certificate tokenBindingCertificate) {
        this.assertion = assertion;
        this.tokenBindingCertificate = tokenBindingCertificate;
    }

    /**
     * Gets the JWT assertion string to use as the {@code client_assertion} parameter.
     *
     * @return the JWT assertion string
     */
    public String assertion() {
        return assertion;
    }

    /**
     * Gets the optional token-binding certificate for mutual-TLS Proof-of-Possession (mTLS PoP).
     * When present, MSAL uses {@code client_assertion_type=jwt-pop} instead of {@code jwt-bearer}.
     *
     * @return the binding certificate, or null if not using mTLS PoP
     */
    public X509Certificate tokenBindingCertificate() {
        return tokenBindingCertificate;
    }
}
