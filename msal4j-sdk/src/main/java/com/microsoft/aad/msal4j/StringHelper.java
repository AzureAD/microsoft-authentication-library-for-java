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

    public static boolean isNullOrBlank(final String str) {
        return str == null || str.trim().length() == 0;
    }
}
