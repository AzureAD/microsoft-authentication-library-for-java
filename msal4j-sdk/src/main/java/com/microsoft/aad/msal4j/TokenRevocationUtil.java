// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.xml.bind.DatatypeConverter;

/**
 * Utility class for token revocation operations
 */
public final class TokenRevocationUtil {

    private TokenRevocationUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Converts a token to its SHA256 hash string representation.
     * This is used in token revocation scenarios to identify tokens without transmitting the original token.
     *
     * @param token The token to hash
     * @return The SHA256 hash of the token as a lowercase hex string
     * @throws MsalClientException If the SHA-256 algorithm is not available
     */
    public static String convertTokenToSHA256HashString(String token) {
        if (token == null || token.isEmpty()) {
            throw new IllegalArgumentException("Token cannot be null or empty");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return DatatypeConverter.printHexBinary(hash).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new MsalClientException("Failed to create SHA-256 hash: " + e.getMessage(),
                    AuthenticationErrorCode.CRYPTO_ERROR);
        }
    }
}
