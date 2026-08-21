// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Holds the extended cache-key components contributed by a request's parameters and lazily
 * computes their hash. Extended cache-key components (for example {@code fmi_path} or
 * {@code client_claims}) isolate token-cache entries so that requests with different component
 * values do not collide.
 * <p>
 * Shared by the {@code *Parameters} classes that expose
 * {@link IAcquireTokenParameters#computeExtCacheKeyHash()} so the storage-and-memoization
 * boilerplate lives in one place instead of being copied per class. The hash is memoized; the
 * components are normally fixed at construction, but a single component may be stamped after
 * construction via {@link #putComponent(String, String)} (used by mTLS Proof-of-Possession to add
 * the resolved binding-certificate KeyId before the silent cache lookup), which invalidates the
 * memoized hash so it is recomputed.
 */
final class ExtendedCacheKey {

    private volatile SortedMap<String, String> components;

    // Lazily memoized hash of the components. Invalidated (set to null) whenever the components
    // change (see putComponent) so it is recomputed on the next computeHash() call.
    private volatile String hashCache;

    ExtendedCacheKey(SortedMap<String, String> components) {
        this.components = components;
    }

    /**
     * Computes the Base64URL-encoded SHA-256 hash of the cache-key components, or an empty string
     * when there are none. The result is memoized.
     */
    String computeHash() {
        String cached = hashCache;
        if (cached != null) {
            return cached;
        }
        String computed = StringHelper.computeExtCacheKeyHash(components);
        hashCache = computed;
        return computed;
    }

    /**
     * Adds or replaces a single cache-key component after construction and invalidates the memoized
     * hash so it is recomputed with the new component. The owning parameters instance may be reused
     * across concurrent acquireToken calls, so the update is copy-on-write (a fresh map is swapped in,
     * never a map another thread may be reading) and synchronized. A blank name or value is ignored.
     */
    synchronized void putComponent(String name, String value) {
        if (StringHelper.isBlank(name) || StringHelper.isBlank(value)) {
            return;
        }
        SortedMap<String, String> updated = this.components == null
                ? new TreeMap<>()
                : new TreeMap<>(this.components);
        updated.put(name, value);
        this.components = updated;
        this.hashCache = null;
    }
}
