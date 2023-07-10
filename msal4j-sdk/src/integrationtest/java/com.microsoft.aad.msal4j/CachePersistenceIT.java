// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.aad.msal4j;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.PlainJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CachePersistenceIT {

    static class TokenPersistence implements ITokenCacheAccessAspect {
        String data;

        TokenPersistence(String data) {
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

    @Test
    void cacheDeserializationSerializationTest() throws IOException, URISyntaxException {
        String dataToInitCache = TestHelper.readResource(this.getClass(), "/cache_data/serialized_cache.json");

        String ID_TOKEN_PLACEHOLDER = "<idToken_placeholder>";
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .audience(Collections.singletonList("jwtAudience"))
                .issuer("issuer")
                .subject("subject")
                .build();
        PlainJWT jwt = new PlainJWT(claimsSet);

        dataToInitCache = dataToInitCache.replace(ID_TOKEN_PLACEHOLDER, jwt.serialize());

        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);

        PublicClientApplication app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        assertEquals(app.getAccounts().join().size(), 1);
        assertEquals(app.tokenCache.accounts.size(), 1);
        assertEquals(app.tokenCache.accessTokens.size(), 2);
        assertEquals(app.tokenCache.refreshTokens.size(), 1);
        assertEquals(app.tokenCache.idTokens.size(), 1);
        assertEquals(app.tokenCache.appMetadata.size(), 1);

        // create new instance of app to make sure in memory cache cleared
        app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        assertEquals(app.getAccounts().join().size(), 1);
        assertEquals(app.tokenCache.accounts.size(), 1);
        assertEquals(app.tokenCache.accessTokens.size(), 2);
        assertEquals(app.tokenCache.refreshTokens.size(), 1);
        assertEquals(app.tokenCache.idTokens.size(), 1);
        assertEquals(app.tokenCache.appMetadata.size(), 1);

        app.removeAccount(app.getAccounts().join().iterator().next()).join();

        assertEquals(app.getAccounts().join().size(), 0);
        assertEquals(app.tokenCache.accounts.size(), 0);
        assertEquals(app.tokenCache.accessTokens.size(), 1);
        assertEquals(app.tokenCache.refreshTokens.size(), 0);
        assertEquals(app.tokenCache.idTokens.size(), 0);
        assertEquals(app.tokenCache.appMetadata.size(), 1);

        app = PublicClientApplication.builder("my_client_id")
                .setTokenCacheAccessAspect(persistenceAspect).build();

        assertEquals(app.getAccounts().join().size(), 0);
        assertEquals(app.tokenCache.accounts.size(), 0);
        assertEquals(app.tokenCache.accessTokens.size(), 1);
        assertEquals(app.tokenCache.refreshTokens.size(), 0);
        assertEquals(app.tokenCache.idTokens.size(), 0);
        assertEquals(app.tokenCache.appMetadata.size(), 1);
    }
}
