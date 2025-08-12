// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

final class StringHelper {

    static String EMPTY_STRING = "";

    static boolean isBlank(final String str) {
        return str == null || str.trim().isEmpty();
    }

    static String createBase64EncodedSha256Hash(String stringToHash) {
        return createSha256Hash(stringToHash, true);
    }

    static String createSha256Hash(String stringToHash) {
        return createSha256Hash(stringToHash, false);
    }

    private static String createSha256Hash(String stringToHash, boolean base64Encode) {
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
        return str == null || str.trim().isEmpty();
    }

    //Converts a map of parameters into a URL query string
    static String serializeQueryParameters(Map<String, String> params) {
        if (params != null && !params.isEmpty()) {
            Map<String, String> encodedParams = urlEncodeMap(params);
            StringBuilder sb = new StringBuilder();

            for (Map.Entry<String, String> entry : encodedParams.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }

                String value = entry.getValue();

                if (sb.length() > 0) {
                    sb.append('&');
                }

                sb.append(entry.getKey());
                sb.append('=');
                sb.append(value);
            }

            return sb.toString();
        } else {
            return "";
        }
    }

    static Map<String, String> urlEncodeMap(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return params;
        } else {
            Map<String, String> out = new LinkedHashMap<>();

            for (Map.Entry<String, String> entry : params.entrySet()) {
                try {
                    String newKey = entry.getKey() != null ? URLEncoder.encode(entry.getKey(), "utf-8") : null;
                    String newValue = entry.getValue() != null ? URLEncoder.encode(entry.getValue(), "utf-8") : null;

                    out.put(newKey, newValue);
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
            }

            return out;
        }
    }

    static Map<String, String> parseQueryParameters(String query) {
        Map<String, String> params = new LinkedHashMap<>();
        if (StringHelper.isBlank(query)) {
            return params;
        } else {
            StringTokenizer st = new StringTokenizer(query.trim(), "&");

            while (st.hasMoreTokens()) {
                String param = st.nextToken();
                String[] pair = param.split("=", 2);

                String key;
                String value;
                try {
                    key = URLDecoder.decode(pair[0], "utf-8");
                    value = pair.length > 1 ? URLDecoder.decode(pair[1], "utf-8") : "";
                } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    continue;
                }

                params.put(key, value);
            }

            return params;
        }
    }

    //Much of the library once used Map<String, List<String>> for query parameters due to reliance on a specific dependency.
    // Although Map<String, String> is now used internally, some public APIs in AuthorizationRequestUrlParameters still return Map<String, List<String>>
    static Map<String, List<String>> convertToMultiValueMap(Map<String, String> singleValueMap) {
        Map<String, List<String>> multiValueMap = new HashMap<>();

        if (singleValueMap != null) {
            for (Map.Entry<String, String> entry : singleValueMap.entrySet()) {
                multiValueMap.put(entry.getKey(), Collections.singletonList(entry.getValue()));
            }
        }

        return multiValueMap;
    }
}