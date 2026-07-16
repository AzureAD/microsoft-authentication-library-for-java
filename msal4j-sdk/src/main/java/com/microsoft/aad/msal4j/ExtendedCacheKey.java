// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.SortedMap;

/**
 * Holds the extended cache-key components contributed by a request's parameters and lazily
 * computes their hash. Extended cache-key components (for example {@code fmi_path} or
 * {@code client_claims}) isolate token-cache entries so that requests with different component
 * values do not collide.
 * <p>
 * Shared by the {@code *Parameters} classes that expose
 * {@link IAcquireTokenParameters#computeExtCacheKeyHash()} so the storage-and-memoization
 * boilerplate lives in one place instead of being copied per class. The hash is memoized because
 * the owning parameters object is immutable after construction.
 */
final class ExtendedCacheKey {

    private final SortedMap<String, String> components;

    // Memoized hash of the components (computed once since the owning parameters are immutable).
    private String hashCache;

    ExtendedCacheKey(SortedMap<String, String> components) {
        this.components = components;
    }

    /**
     * Computes the Base64URL-encoded SHA-256 hash of the cache-key components, or an empty string
     * when there are none. The result is memoized.
     */
    String computeHash() {
        if (hashCache == null) {
            hashCache = StringHelper.computeExtCacheKeyHash(components);
        }
        return hashCache;
    }
}
