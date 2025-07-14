// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing the context in which the token cache is accessed.
 * <p>
 * When MSAL accesses the token cache (before or after), it provides an instance of this interface
 * to the application through {@link ITokenCacheAccessAspect} methods. This allows the application
 * to get information about the cache access operation being performed, such as which client and account
 * are involved, and whether the operation modified the cache.
 * <p>
 * This context is particularly useful for applications implementing token cache serialization and
 * persistence strategies, as it helps determine when the cache should be loaded from or saved to
 * persistent storage.
 * <p>
 * For more details, see https://aka.ms/msal4j-token-cache
 */
public interface ITokenCacheAccessContext {

    /**
     * Gets the token cache instance being accessed.
     * <p>
     * This can be used to read or modify the cache entries during a cache access operation.
     *
     * @return The {@link ITokenCache} instance that is being accessed.
     */
    ITokenCache tokenCache();

    /**
     * Gets the client ID associated with the current cache access operation.
     * <p>
     * This identifies which client application (registered in Azure AD) is performing
     * the cache access. In multi-tenant or multi-client scenarios, applications may want
     * to partition their token cache by client ID.
     *
     * @return The client ID (application ID) as registered in the Azure portal.
     */
    String clientId();

    /**
     * Gets the account associated with the current cache access operation, if any.
     * <p>
     * This may be null for operations that are not specific to a user account, such as
     * client credential flow token requests.
     *
     * @return The {@link IAccount} instance related to this cache access, or null if
     *         the operation is not account-specific.
     */
    IAccount account();

    /**
     * Indicates whether the cache was modified during this operation.
     * <p>
     * This is particularly useful in the {@link ITokenCacheAccessAspect#afterCacheAccess} method
     * to determine whether the cache should be persisted. If no changes were made to the cache,
     * applications can avoid unnecessary write operations.
     *
     * @return true if the cache was modified during this operation, false otherwise.
     */
    boolean hasCacheChanged();
}
