// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Simple in-memory token cache persistence for integration tests.
 * Implements ITokenCacheAccessAspect to serialize/deserialize cache data
 * across application instances.
 */
class TestTokenCachePersistence implements ITokenCacheAccessAspect {
    String data;

    TestTokenCachePersistence(String data) {
        this.data = data;
    }

    @Override
    public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        iTokenCacheAccessContext.tokenCache().deserialize(data);
    }

    @Override
    public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        data = iTokenCacheAccessContext.tokenCache().serialize();
    }
}
