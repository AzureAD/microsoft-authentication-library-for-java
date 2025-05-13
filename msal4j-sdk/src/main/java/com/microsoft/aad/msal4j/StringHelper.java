// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

final class StringHelper {

    static String EMPTY_STRING = "";

    static boolean isBlank(final String str) {
        return str == null || str.trim().length() == 0;
    }

    static String createBase64EncodedSha256Hash(String stringToHash) {
        return createSha256Hash(stringToHash, true);
    }

    static String createSha256Hash(String stringToHash) {
        return createSha256Hash(stringToHash, false);
    }

    static private String createSha256Hash(String stringToHash, boolean base64Encode) {
        String res;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));

            if (base64Encode) {
                res = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            } else {
                res = new String(hash, StandardCharsets.UTF_8);
            }
        } catch (NoSuchAlgorithmException e) {
            res = null;
        }
        return res;
    }

    /**
     * Creates a SHA-256 hash of the input string and returns it as a lowercase hex string.
     * This is used for token revocation and other scenarios requiring hex hash representation.
     *
     * @param stringToHash The string to hash
     * @return The SHA-256 hash of the string as a lowercase hex string
     * @throws MsalClientException If the SHA-256 algorithm is not available
     */
    static String createSha256HashHexString(String stringToHash) {
        if (stringToHash == null || stringToHash.isEmpty()) {
            throw new IllegalArgumentException("String to hash cannot be null or empty");
        }

        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            byte[] hash = messageDigest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new MsalClientException("Failed to create SHA-256 hash: " + e.getMessage(),
                    AuthenticationErrorCode.CRYPTO_ERROR);
        }
    }

    static boolean isNullOrBlank(final String str) {
        return str == null || str.trim().length() == 0;
    }
}
