// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import static com.microsoft.aad.msal4j.labapi.KeyVaultSecrets.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenCacheIT {

    @Test
    void singleAccountInCache_RemoveAccountTest() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(APP_PCACLIENT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        PublicClientApplication pca = PublicClientApplication.builder(
                app.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        // Check that cache is empty
        assertEquals(pca.getAccounts().join().size(), 0);

        Map<String, String> extraQueryParameters = new HashMap<>();
        extraQueryParameters.put("test", "test");

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                        .extraQueryParameters(extraQueryParameters)
                .build())
                .get();

        // Check that cache contains one account
        assertEquals(pca.getAccounts().join().size(), 1);

        pca.removeAccount(pca.getAccounts().join().iterator().next()).join();

        // Check that account has been removed
        assertEquals(pca.getAccounts().join().size(), 0);
    }

    @Test
    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
    void twoAccountsInCache_SameUserDifferentTenants_RemoveAccountTest() throws Exception {

        AppConfig app = LabResponseHelper.getAppConfig(APP_PCACLIENT);
        UserConfig guestUser = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        String dataToInitCache = TestHelper.readResource(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");

        // check that cache is empty
        assertEquals(dataToInitCache, "");

        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);

        // acquire tokens for home tenant, and serialize cache
        PublicClientApplication pca = PublicClientApplication.builder(
                app.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY)
                .setTokenCacheAccessAspect(persistenceAspect)
                .build();

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        guestUser.getHomeUPN(),
                        guestUser.getPassword().toCharArray())
                .build())
                .get();

        String guestTenantAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + guestUser.getTenantId();

        // initialize pca with tenant where user is guest, deserialize cache, and acquire second token
        PublicClientApplication pca2 = PublicClientApplication.builder(
                app.getAppId()).
                authority(guestTenantAuthority).
                setTokenCacheAccessAspect(persistenceAspect).
                build();

        pca2.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        guestUser.getHomeUPN(),
                        guestUser.getPassword().toCharArray())
                .build())
                .get();

        // There should be two tokens in cache, with same accounts except for tenant
        assertEquals(pca2.getAccounts().join().iterator().next().getTenantProfiles().size(), 2);

        IAccount account = pca2.getAccounts().get().iterator().next();

        // RemoveAccount should remove both cache entities
        pca2.removeAccount(account).join();

        assertEquals(0, pca2.getAccounts().join().size());

        //clean up file
        TestHelper.deleteFileContent(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");
    }

    private static class TokenPersistence implements ITokenCacheAccessAspect {
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
}
