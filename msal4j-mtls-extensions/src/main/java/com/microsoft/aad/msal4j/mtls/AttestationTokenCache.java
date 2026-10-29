// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.azure.json.JsonProviders;
import com.azure.json.JsonReader;
import com.azure.json.JsonToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class AttestationTokenCache {

    private static final long FRESHNESS_BUFFER_SECONDS = 5L * 60L;
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<String, Object> locks = new ConcurrentHashMap<>();

    String getOrAttest(
            String endpoint,
            String keyId,
            AttestationOperation operation) {
        String cacheKey = normalizeEndpoint(endpoint) + "|" + keyId;
        Entry cached = entries.get(cacheKey);
        if (isFresh(cached)) {
            return cached.jwt;
        }

        Object lock = locks.computeIfAbsent(cacheKey, ignored -> new Object());
        synchronized (lock) {
            cached = entries.get(cacheKey);
            if (isFresh(cached)) {
                return cached.jwt;
            }
            String jwt = operation.attest();
            if (jwt == null || jwt.trim().isEmpty()) {
                throw new MtlsMsiException(
                        "KeyGuard attestation failed; no attestation token was produced.");
            }
            long expiresOn = readExpiry(jwt);
            if (expiresOn <= currentEpochSeconds() + FRESHNESS_BUFFER_SECONDS) {
                throw new MtlsMsiException(
                        "KeyGuard attestation returned an expired or insufficiently fresh token.");
            }
            entries.put(cacheKey, new Entry(jwt, expiresOn));
            return jwt;
        }
    }

    private static boolean isFresh(Entry entry) {
        return entry != null
                && entry.expiresOn > currentEpochSeconds() + FRESHNESS_BUFFER_SECONDS;
    }

    private static long readExpiry(String jwt) throws MtlsMsiException {
        try {
            String[] segments = jwt.split("\\.");
            if (segments.length < 2) {
                throw new IllegalArgumentException("JWT has fewer than two segments");
            }
            String payload = new String(
                    Base64.getUrlDecoder().decode(padBase64(segments[1])),
                    StandardCharsets.UTF_8);
            try (JsonReader jsonReader = JsonProviders.createReader(payload)) {
                Long expiresOn = jsonReader.readObject(reader -> {
                    Long expiry = null;
                    while (reader.nextToken() != JsonToken.END_OBJECT) {
                        String field = reader.getFieldName();
                        JsonToken token = reader.nextToken();
                        if ("exp".equals(field)) {
                            if (token != JsonToken.NUMBER) {
                                throw new IllegalArgumentException("JWT exp claim must be numeric.");
                            }
                            expiry = reader.getLong();
                        } else {
                            reader.skipChildren();
                        }
                    }
                    return expiry;
                });
                if (expiresOn == null || jsonReader.nextToken() != JsonToken.END_DOCUMENT) {
                    throw new IllegalArgumentException("JWT must contain a numeric exp claim.");
                }
                return expiresOn;
            }
        } catch (IOException | IllegalArgumentException | IllegalStateException e) {
            throw new MtlsMsiException(
                    "Unable to determine KeyGuard attestation token expiry.", e);
        }
    }

    private static String padBase64(String value) {
        int remainder = value.length() % 4;
        if (remainder == 0) {
            return value;
        }
        return value + (remainder == 2 ? "==" : "=");
    }

    private static String normalizeEndpoint(String endpoint) {
        String normalized = endpoint.trim().toLowerCase();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static long currentEpochSeconds() {
        return System.currentTimeMillis() / 1000L;
    }

    interface AttestationOperation {
        String attest();
    }

    private static final class Entry {
        final String jwt;
        final long expiresOn;

        Entry(String jwt, long expiresOn) {
            this.jwt = jwt;
            this.expiresOn = expiresOn;
        }
    }
}
