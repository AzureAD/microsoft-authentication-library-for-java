// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
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
