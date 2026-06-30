// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

import static com.microsoft.aad.msal4j.Constants.POINT_DELIMITER;

class AccountTest {

    private static String getTestIdToken(String environment, String tenant) {
        String claims = "{\n" +
                "  \"iss\": \"" + environment + "\",\n" +
                "  \"tid\": \"" + tenant + "\"\n" +
                "}";

        String encodedIdToken = new String(Base64.getEncoder().encode(claims.getBytes()), StandardCharsets.UTF_8);

        encodedIdToken = TestHelper.getJWTHeaderBase64EncodedJson() + POINT_DELIMITER +
                encodedIdToken + POINT_DELIMITER +
                TestHelper.getEmptyBase64EncodedJson();

        return encodedIdToken;
    }

    @Test
    void multiCloudAccount_aggregatedInGetAccountsRemoveAccountApis() throws Exception {
        String BLACK_FORESRT_TENANT = "de_tid";
        String WW_TENTANT = "tid";
        String BLACK_FOREST_ENV = "login.microsoftonline.de";
        String WW_ENV = "login.microsoftonline.com";
        String CLIENT_ID = "client_id";
        String DE_ID_TOKEN_PLACEHOLDER = "<de_id_token_placeholder>";
        String ID_TOKEN_PLACEHOLDER = "<id_token_placeholder>";

        String cacheWithMultiCloudAccount = TestHelper.readResource(
                this.getClass(), "/cache_data/multi-cloud-account-cache.json");

        cacheWithMultiCloudAccount = cacheWithMultiCloudAccount.replace
                (DE_ID_TOKEN_PLACEHOLDER, getTestIdToken(BLACK_FOREST_ENV, BLACK_FORESRT_TENANT));

        cacheWithMultiCloudAccount = cacheWithMultiCloudAccount.replace
                (ID_TOKEN_PLACEHOLDER, getTestIdToken(WW_ENV, WW_TENTANT));

        ITokenCacheAccessAspect persistenceAspect = new ITokenCacheAccessAspect() {
            String data;

            @Override
            public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
                iTokenCacheAccessContext.tokenCache().deserialize(data);
            }

            @Override
            public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
                data = iTokenCacheAccessContext.tokenCache().serialize();
            }

            ITokenCacheAccessAspect init(String data) {
                this.data = data;
                return this;
            }
        }.init(cacheWithMultiCloudAccount);

        // acquire tokens for home tenant, and serialize cache
        PublicClientApplication pca = PublicClientApplication.builder(
                CLIENT_ID).
                authority(TestConstants.COMMON_AUTHORITY)
                .setTokenCacheAccessAspect(persistenceAspect)
                .build();

        Set<IAccount> accounts = pca.getAccounts().join();

        assertEquals(1, accounts.size());
        IAccount account = accounts.iterator().next();

        Map<String, ITenantProfile> tenantProfiles = account.getTenantProfiles();
        assertEquals(2, tenantProfiles.size());

        assertTrue(tenantProfiles.containsKey(BLACK_FORESRT_TENANT));
        assertTrue(tenantProfiles.containsKey(WW_TENTANT));

        pca.removeAccount(account).join();
        accounts = pca.getAccounts().join();
        assertEquals(0, accounts.size());

        assertEquals(0, pca.tokenCache.accounts.size());
        assertEquals(0, pca.tokenCache.idTokens.size());
        assertEquals(0, pca.tokenCache.refreshTokens.size());
        assertEquals(0, pca.tokenCache.accessTokens.size());
    }
}
