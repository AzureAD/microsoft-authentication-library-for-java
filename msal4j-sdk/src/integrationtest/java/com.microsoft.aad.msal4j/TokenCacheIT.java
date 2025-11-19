// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenCacheIT {

    @Test
    void singleAccountInCache_RemoveAccountTest() throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser(AzureEnvironment.AZURE);
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
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

    // TODO: labapi2 doesn't have ADFS v4 specific user helper yet - will be pulled from MSAL.NET
//    @Test
//    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
//    void twoAccountsInCache_RemoveAccountTest() throws Exception {
//
//        LabResponse managedResponse = LabUserHelper.getDefaultUser(AzureEnvironment.AZURE);
//        LabUser managedUser = managedResponse.getUser();
//
//        PublicClientApplication pca = PublicClientApplication.builder(
//                managedResponse.getApp().getAppId()).
//                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
//                build();
//
//        assertEquals(pca.getAccounts().join().size(), 0);
//
//        pca.acquireToken(UserNamePasswordParameters.
//                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
//                        managedUser.getUpn(),
//                        managedUser.getPassword().toCharArray())
//                .build())
//                .get();
//
//        assertEquals(pca.getAccounts().join().size(), 1);
//
//        // get lab user for different account
//        LabResponse adfsResponse = LabUserHelper.getFederatedAdfsUser(AzureEnvironment.AZURE, LabServiceParameters.FederationProvider.ADFS_V4);
//        LabUser adfsUser = adfsResponse.getUser();
//
//        // acquire token for different account
//        pca.acquireToken(UserNamePasswordParameters.
//                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
//                        adfsUser.getUpn(),
//                        adfsUser.getPassword().toCharArray())
//                .build())
//                .get();
//
//        assertEquals(pca.getAccounts().join().size(), 2);
//
//        Set<IAccount> accounts = pca.getAccounts().join();
//        IAccount accountLabResponse1 = accounts.stream().filter(
//                x -> x.username().equalsIgnoreCase(
//                        managedUser.getUpn())).findFirst().orElse(null);
//
//        pca.removeAccount(accountLabResponse1).join();
//
//        assertEquals(pca.getAccounts().join().size(), 1);
//
//        IAccount accountLabResponse2 = pca.getAccounts().get().iterator().next();
//
//        // Check that the right account was left in the cache
//        assertEquals(accountLabResponse2.username(), adfsUser.getUpn());
//    }

    // TODO: labapi2 doesn't have guest user configuration yet - will be pulled from MSAL.NET
//    @Test
//    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
//    void twoAccountsInCache_SameUserDifferentTenants_RemoveAccountTest() throws Exception {
//
//        UserQuery query = new UserQuery();
//        query.setUserType(LabServiceParameters.UserType.GUEST);
//
//        LabResponse labResponse = LabUserHelper.getLabUserData(query);
//        LabUser guestUser = labResponse.getUser();
//        Lab lab = labResponse.getLab();
//
//        String dataToInitCache = TestHelper.readResource(
//                this.getClass(),
//                "/cache_data/remove-account-test-cache.json");
//
//        // check that cache is empty
//        assertEquals(dataToInitCache, "");
//
//        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);
//
//        // acquire tokens for home tenant, and serialize cache
//        PublicClientApplication pca = PublicClientApplication.builder(
//                guestUser.getAppId()).
//                authority(TestConstants.ORGANIZATIONS_AUTHORITY)
//                .setTokenCacheAccessAspect(persistenceAspect)
//                .build();
//
//        pca.acquireToken(UserNamePasswordParameters.
//                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
//                        guestUser.getHomeUPN(),
//                        guestUser.getPassword().toCharArray())
//                .build())
//                .get();
//
//        String guestTenantAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + lab.getTenantId();
//
//        // initialize pca with tenant where user is guest, deserialize cache, and acquire second token
//        PublicClientApplication pca2 = PublicClientApplication.builder(
//                guestUser.getAppId()).
//                authority(guestTenantAuthority).
//                setTokenCacheAccessAspect(persistenceAspect).
//                build();
//
//        pca2.acquireToken(UserNamePasswordParameters.
//                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
//                        guestUser.getHomeUPN(),
//                        guestUser.getPassword().toCharArray())
//                .build())
//                .get();
//
//        // There should be two tokens in cache, with same accounts except for tenant
//        assertEquals(pca2.getAccounts().join().iterator().next().getTenantProfiles().size(), 2);
//
//        IAccount account = pca2.getAccounts().get().iterator().next();
//
//        // RemoveAccount should remove both cache entities
//        pca2.removeAccount(account).join();
//
//        assertEquals(0, pca2.getAccounts().join().size());
//
//        //clean up file
//        TestHelper.deleteFileContent(
//                this.getClass(),
//                "/cache_data/remove-account-test-cache.json");
//    }

    // TODO: labapi2 doesn't have on-prem ADFS user configuration yet - will be pulled from MSAL.NET
//    @Test
//    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
//    void retrieveAccounts_ADFSOnPrem() throws Exception {
//        UserQuery query = new UserQuery();
//        query.setFederationProvider(LabServiceParameters.FederationProvider.ADFS_V2019);
//        query.setUserType(LabServiceParameters.UserType.ON_PREM);
//
//        LabResponse labResponse = LabUserHelper.getLabUserData(query);
//        LabUser user = labResponse.getUser();
//
//        PublicClientApplication pca = PublicClientApplication.builder(
//                        TestConstants.ADFS_APP_ID).
//                authority(TestConstants.ADFS_AUTHORITY).
//                build();
//
//        pca.acquireToken(UserNamePasswordParameters.
//                        builder(Collections.singleton(TestConstants.ADFS_SCOPE),
//                                user.getUpn(),
//                                user.getPassword().toCharArray())
//                        .build())
//                .get();
//
//        assertNotNull(pca.getAccounts().join().iterator().next());
//        assertEquals(pca.getAccounts().join().size(), 1);
//    }


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
