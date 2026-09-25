// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Access-token scheme returned by the identity provider.
 */
public enum TokenType {
    BEARER("Bearer"),
    MTLS_POP("mtls_pop");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
