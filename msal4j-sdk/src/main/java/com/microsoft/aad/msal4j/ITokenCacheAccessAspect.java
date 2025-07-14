// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing operation of executing code before and after cache access.
 * <p>
 * This interface enables applications to hook into the token cache access operations,
 * allowing for serialization and persistence of the token cache. Implementing this interface
 * allows applications to load the cache state from persistent storage before MSAL accesses it,
 * and save changes back to persistent storage after MSAL modifies it.
 * <p>
 * For more details, see https://aka.ms/msal4j-token-cache
 */
public interface ITokenCacheAccessAspect {

    /**
     * Executes before the token cache is accessed by MSAL.
     * <p>
     * This method is called by MSAL immediately before it accesses the token cache.
     * Applications should use this method to load the current cache state from persistent
     * storage and update the in-memory cache by calling {@link ITokenCacheAccessContext#tokenCache()}.
     * This ensures MSAL has access to all previously cached tokens.
     *
     * @param iTokenCacheAccessContext Context object providing access to the token cache and related information.
     */
    void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext);

    /**
     * Executes after the token cache is accessed by MSAL.
     * <p>
     * This method is called by MSAL immediately after it accesses (and potentially modifies)
     * the token cache. Applications should use this method to persist the current state of the
     * cache to storage if changes were made. The {@link ITokenCacheAccessContext#hasCacheChanged()}
     * method can be used to determine if the cache was modified during the operation.
     *
     * @param iTokenCacheAccessContext Context object providing access to the token cache and related information,
     *                                 including whether the cache was modified.
     */
    void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext);
}
