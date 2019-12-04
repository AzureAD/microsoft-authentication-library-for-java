// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

public class TokenCacheIT {

    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void singleAccountInCache_RemoveAccountTest() throws Exception {
        User user = labUserProvider.getDefaultUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        // Check that cache is empty
        Assert.assertEquals(pca.getAccounts().join().size() , 0);

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        // Check that cache contains one account
        Assert.assertEquals(pca.getAccounts().join().size(), 1);

        pca.removeAccount(pca.getAccounts().join().iterator().next()).join();

        // Check that account has been removed
        Assert.assertEquals(pca.getAccounts().join().size(), 0);
    }

    @Test
    public void twoAccountsInCache_RemoveAccountTest() throws Exception{

        User managedUser = labUserProvider.getDefaultUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                managedUser.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        Assert.assertEquals(pca.getAccounts().join().size() , 0);

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        managedUser.getUpn(),
                        managedUser.getPassword().toCharArray())
                .build())
                .get();

        Assert.assertEquals(pca.getAccounts().join().size() , 1);

        // get lab user for different account
        User adfsUser = labUserProvider.getFederatedAdfsUser(FederationProvider.ADFS_4);

        // acquire token for different account
        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        adfsUser.getUpn(),
                        adfsUser.getPassword().toCharArray())
                .build())
                .get();

        Assert.assertEquals(pca.getAccounts().join().size(), 2);

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount accountLabResponse1 = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        managedUser.getUpn())).findFirst().orElse(null);

        pca.removeAccount(accountLabResponse1).join();

        Assert.assertEquals(pca.getAccounts().join().size() , 1);

        IAccount accountLabResponse2 = pca.getAccounts().get().iterator().next();

        // Check that the right account was left in the cache
        Assert.assertEquals(accountLabResponse2.username(), adfsUser.getUpn());
    }

    //@Test
    // TODO guest user upn returned from lab contains # so user discovery request
    // fails, in upn url encoded userrealm fails to identify user
    public void twoAccountsInCache_SameUserDifferentTenants_RemoveAccountTest() throws Exception{

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.GUEST);

        User guestUser =  labUserProvider.getLabUser(query);
        Lab lab = LabService.getLab(guestUser.getLabName());

        String dataToInitCache = TestHelper.readResource(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");

        // check that cache is empty
        Assert.assertEquals(dataToInitCache, "");

        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);

        // acquire tokens for home tenant, and serialize cache
        PublicClientApplication pca = PublicClientApplication.builder(
                guestUser.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY)
                .setTokenCacheAccessAspect(persistenceAspect)
                .build();

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        guestUser.getUpn(),
                        guestUser.getPassword().toCharArray())
                .build())
                .get();

        String guestTenantAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + lab.getTenantId();

        // initialize pca with tenant where user is guest, deserialize cache, and acquire second token
        PublicClientApplication pca2 = PublicClientApplication.builder(
                guestUser.getAppId()).
                authority(guestTenantAuthority).
                setTokenCacheAccessAspect(persistenceAspect).
                build();

        pca2.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        guestUser.getUpn(),
                        guestUser.getPassword().toCharArray())
                .build())
                .get();

        // There should be two tokens in cache, with same accounts except for tenant
        Assert.assertEquals(pca2.getAccounts().join().size() , 2);

        IAccount account = pca2.getAccounts().get().iterator().next();

        // RemoveAccount should remove both cache entities
        pca2.removeAccount(account).join();

        Assert.assertEquals(pca.getAccounts().join().size() , 0);

        //clean up file
        TestHelper.deleteFileContent(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");
    }

    private static class TokenPersistence implements ITokenCacheAccessAspect{
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
}
