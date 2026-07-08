// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Represents the type of an access token returned by the identity provider.
 *
 * <p>A {@link #BEARER} token can be used by any caller that possesses it. An {@link #MTLS_POP}
 * (mutual-TLS Proof-of-Possession) token is cryptographically bound to a certificate and can only be
 * used by a caller that can prove possession of that certificate on the TLS connection to the resource.
 *
 * <p>The token type of a result is available via {@link AuthenticationResultMetadata#tokenType()}.
 * For more details on mTLS Proof-of-Possession, see https://aka.ms/msal4j-pop
 */
public enum TokenType {

    /**
     * A standard Bearer access token. This is the default token type.
     */
    BEARER("Bearer"),

    /**
     * A mutual-TLS Proof-of-Possession access token, cryptographically bound to the client certificate
     * presented on the TLS handshake to the token endpoint (bound via {@code cnf}/{@code x5t#S256}).
     */
    MTLS_POP("mtls_pop");

    private final String value;

    TokenType(String value) {
        this.value = value;
    }

    /**
     * @return the wire/string representation of this token type
     */
    public String value() {
        return value;
    }

    /**
     * Maps a token_type string (as returned in a token response) to a {@link TokenType}.
     * Unknown or null values map to {@link #BEARER}.
     *
     * @param tokenType the token_type string from a token response
     * @return the corresponding {@link TokenType}, defaulting to {@link #BEARER}
     */
    static TokenType fromString(String tokenType) {
        if (StringHelper.isBlank(tokenType)) {
            return BEARER;
        }

        if (MTLS_POP.value.equalsIgnoreCase(tokenType) || "pop".equalsIgnoreCase(tokenType)) {
            return MTLS_POP;
        }

        return BEARER;
    }

    @Override
    public String toString() {
        return value;
    }
}
