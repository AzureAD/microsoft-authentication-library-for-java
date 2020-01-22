// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

final class StringHelper {

    static boolean isBlank(final String str) {
        return str == null || str.trim().length() == 0;
    }

    static String createBase64EncodedSha256Hash(String stringToHash){
        String base64EncodedSha256Hash;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedString = digest.digest(stringToHash.getBytes(StandardCharsets.UTF_8));
            base64EncodedSha256Hash = Base64.getUrlEncoder().withoutPadding().encodeToString(hashedString);
        } catch(NoSuchAlgorithmException e){
            base64EncodedSha256Hash = null;
        }
        return base64EncodedSha256Hash;
    }


}
