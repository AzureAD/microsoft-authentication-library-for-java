// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing state of token cache
 */
public interface ITokenCacheAccessContext {

    /**
     * @return instance of accessed ITokenCache
     */
    ITokenCache tokenCache();

    /**
     * @return client id used for cache access
     */
    String clientId();

    /**
     * @return instance of IAccount used for cache access
     */
    IAccount account();

    /**
     * @return a boolean value telling whether cache was changed
     */
    boolean hasCacheChanged();
}
