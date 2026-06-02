// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CachePersistenceIT {

    @Test
    void cacheDeserializationSerializationTest() {
        String dataToInitCache = TestHelper.readResource(this.getClass(), "/cache_data/serialized_cache.json");
        dataToInitCache = dataToInitCache.replace("<idToken_placeholder>", TestHelper.ENCODED_JWT);

        ITokenCacheAccessAspect persistenceAspect = new TestTokenCachePersistence(dataToInitCache);

        PublicClientApplication app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        assertEquals(1, app.getAccounts().join().size());
        assertEquals(1, app.tokenCache.accounts.size());
        assertEquals(1, app.tokenCache.accessTokens.size());
        assertEquals(1, app.tokenCache.refreshTokens.size());
        assertEquals(1, app.tokenCache.idTokens.size());
        assertEquals(1, app.tokenCache.appMetadata.size());

        // create new instance of app to make sure in memory cache cleared
        app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        assertEquals(1, app.getAccounts().join().size());
        assertEquals(1, app.tokenCache.accounts.size());
        assertEquals(1, app.tokenCache.accessTokens.size());
        assertEquals(1, app.tokenCache.refreshTokens.size());
        assertEquals(1, app.tokenCache.idTokens.size());
        assertEquals(1, app.tokenCache.appMetadata.size());

        app.removeAccount(app.getAccounts().join().iterator().next()).join();

        assertEquals(0, app.getAccounts().join().size());
        assertEquals(0, app.tokenCache.accounts.size());
        assertEquals(0, app.tokenCache.accessTokens.size());
        assertEquals(0, app.tokenCache.refreshTokens.size());
        assertEquals(0, app.tokenCache.idTokens.size());
        assertEquals(1, app.tokenCache.appMetadata.size());

        app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        assertEquals(0, app.getAccounts().join().size());
        assertEquals(0, app.tokenCache.accounts.size());
        assertEquals(0, app.tokenCache.accessTokens.size());
        assertEquals(0, app.tokenCache.refreshTokens.size());
        assertEquals(0, app.tokenCache.idTokens.size());
        assertEquals(1, app.tokenCache.appMetadata.size());
    }
}
