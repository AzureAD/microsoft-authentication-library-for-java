// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URISyntaxException;

public class CachePersistenceIT {

    static class TokenPersistence implements ITokenCacheAccessAspect{
        String data;

        TokenPersistence(String data){
            this.data = data;
        }

        @Override
        public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext){
            iTokenCacheAccessContext.tokenCache().deserialize(data);
        }

        @Override
        public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
            data = iTokenCacheAccessContext.tokenCache().serialize();
        }
    }

    @Test
    public void cacheDeserializationSerializationTest() throws IOException, URISyntaxException {
        String dataToInitCache = TestHelper.readResource(this.getClass(), "/cache_data/serialized_cache.json");

        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);

        PublicClientApplication app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        Assert.assertEquals(app.getAccounts().join().size() , 1);
        Assert.assertEquals(app.tokenCache.accounts.size(), 1);
        Assert.assertEquals(app.tokenCache.accessTokens.size(), 2);
        Assert.assertEquals(app.tokenCache.refreshTokens.size(), 1);
        Assert.assertEquals(app.tokenCache.idTokens.size(), 1);
        Assert.assertEquals(app.tokenCache.appMetadata.size(), 1);

        // create new instance of app to make sure in memory cache cleared
        app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        Assert.assertEquals(app.getAccounts().join().size() , 1);
        Assert.assertEquals(app.tokenCache.accounts.size(), 1);
        Assert.assertEquals(app.tokenCache.accessTokens.size(), 2);
        Assert.assertEquals(app.tokenCache.refreshTokens.size(), 1);
        Assert.assertEquals(app.tokenCache.idTokens.size(), 1);
        Assert.assertEquals(app.tokenCache.appMetadata.size(), 1);

        app.removeAccount(app.getAccounts().join().iterator().next()).join();

        Assert.assertEquals(app.getAccounts().join().size() , 0);
        Assert.assertEquals(app.tokenCache.accounts.size(), 0);
        Assert.assertEquals(app.tokenCache.accessTokens.size(), 1);
        Assert.assertEquals(app.tokenCache.refreshTokens.size(), 0);
        Assert.assertEquals(app.tokenCache.idTokens.size(), 0);
        Assert.assertEquals(app.tokenCache.appMetadata.size(), 1);

        app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        Assert.assertEquals(app.getAccounts().join().size() , 0);
        Assert.assertEquals(app.tokenCache.accounts.size(), 0);
        Assert.assertEquals(app.tokenCache.accessTokens.size(), 1);
        Assert.assertEquals(app.tokenCache.refreshTokens.size(), 0);
        Assert.assertEquals(app.tokenCache.idTokens.size(), 0);
        Assert.assertEquals(app.tokenCache.appMetadata.size(), 1);
    }
}
