// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenCacheIT {

    private LabUserProvider labUserProvider;

    @BeforeAll
    void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    void singleAccountInCache_RemoveAccountTest() throws Exception {
        User user = labUserProvider.getDefaultUser();

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

    @Test
    void twoAccountsInCache_RemoveAccountTest() throws Exception {

        User managedUser = labUserProvider.getDefaultUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                managedUser.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        assertEquals(pca.getAccounts().join().size(), 0);

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        managedUser.getUpn(),
                        managedUser.getPassword().toCharArray())
                .build())
                .get();

        assertEquals(pca.getAccounts().join().size(), 1);

        // get lab user for different account
        User adfsUser = labUserProvider.getFederatedAdfsUser(FederationProvider.ADFS_4);

        // acquire token for different account
        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        adfsUser.getUpn(),
                        adfsUser.getPassword().toCharArray())
                .build())
                .get();

        assertEquals(pca.getAccounts().join().size(), 2);

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount accountLabResponse1 = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        managedUser.getUpn())).findFirst().orElse(null);

        pca.removeAccount(accountLabResponse1).join();

        assertEquals(pca.getAccounts().join().size(), 1);

        IAccount accountLabResponse2 = pca.getAccounts().get().iterator().next();

        // Check that the right account was left in the cache
        assertEquals(accountLabResponse2.username(), adfsUser.getUpn());
    }

    @Test
    void twoAccountsInCache_SameUserDifferentTenants_RemoveAccountTest() throws Exception {

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.GUEST);

        User guestUser = labUserProvider.getLabUser(query);
        Lab lab = LabService.getLab(guestUser.getLabName());

        String dataToInitCache = TestHelper.readResource(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");

        // check that cache is empty
        assertEquals(dataToInitCache, "");

        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);

        // acquire tokens for home tenant, and serialize cache
        PublicClientApplication pca = PublicClientApplication.builder(
                guestUser.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY)
                .setTokenCacheAccessAspect(persistenceAspect)
                .build();

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        guestUser.getHomeUPN(),
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
                        guestUser.getHomeUPN(),
                        guestUser.getPassword().toCharArray())
                .build())
                .get();

        // There should be two tokens in cache, with same accounts except for tenant
        assertEquals(pca2.getAccounts().join().iterator().next().getTenantProfiles().size(), 2);

        IAccount account = pca2.getAccounts().get().iterator().next();

        // RemoveAccount should remove both cache entities
        pca2.removeAccount(account).join();

        assertEquals(pca.getAccounts().join().size(), 0);

        //clean up file
        TestHelper.deleteFileContent(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");
    }

    @Test
    void retrieveAccounts_ADFSOnPrem() throws Exception {
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.ADFS_2019);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.ON_PREM);

        User user = labUserProvider.getLabUser(query);

        PublicClientApplication pca = PublicClientApplication.builder(
                        TestConstants.ADFS_APP_ID).
                authority(TestConstants.ADFS_AUTHORITY).
                build();

        pca.acquireToken(UserNamePasswordParameters.
                        builder(Collections.singleton(TestConstants.ADFS_SCOPE),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .build())
                .get();

        assertNotNull(pca.getAccounts().join().iterator().next());
        assertEquals(pca.getAccounts().join().size(), 1);
    }

    @Test
    void testStaticCache() throws Exception {
        AppCredentialProvider appProvider = new AppCredentialProvider(AzureEnvironment.AZURE);
        final String clientId = appProvider.getLabVaultAppId();
        final String password = appProvider.getLabVaultPassword();
        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        //Create three client applications: one that uses its own instance of a TokenCache,
        // and two that use the useSharedCache option to use the same static TokenCache
        ConfidentialClientApplication cca_notStatic = ConfidentialClientApplication.builder(
                        clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();

        ConfidentialClientApplication cca_sharedCache1 = ConfidentialClientApplication.builder(
                        clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                useSharedCache(true).
                build();

        ConfidentialClientApplication cca_sharedCache2 = ConfidentialClientApplication.builder(
                        clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                useSharedCache(true).
                build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build();

        //Make a number of token calls using the different ConfidentialClientApplications
        //  1. Retrieve and cache new tokens using the ConfidentialClientApplication that does not use the shared cache
        IAuthenticationResult result_notStatic1 = cca_notStatic.acquireToken(parameters).get();
        //  2. The client credential flow does a cache lookup by default, so making the same acquireToken call should retrieve the tokens cached during call 1
        IAuthenticationResult result_notStatic2 = cca_notStatic.acquireToken(parameters).get();
        //  3. Retrieve and cache new tokens using the ConfidentialClientApplication that uses the static cache
        IAuthenticationResult result_sharedCache1 = cca_sharedCache1.acquireToken(parameters).get();
        //  4. Due to using the static cache this should behave like token call 2 and retrieve the tokens cached in call 3
        IAuthenticationResult result_sharedCache2 = cca_sharedCache2.acquireToken(parameters).get();

        assertNotNull(result_notStatic1);
        assertNotNull(result_notStatic1.accessToken());
        assertNotNull(result_notStatic2);
        assertNotNull(result_notStatic2.accessToken());
        assertNotNull(result_sharedCache1);
        assertNotNull(result_sharedCache1.accessToken());
        assertNotNull(result_sharedCache2);
        assertNotNull(result_sharedCache2.accessToken());

        //None of the tokens retrieved using cca_notStatic should be the same as those retrieved using cca_sharedCache1 or cca_sharedCache2
        assertNotEquals(result_notStatic1.accessToken(), result_sharedCache1.accessToken());
        assertNotEquals(result_notStatic1.accessToken(), result_sharedCache2.accessToken());
        assertNotEquals(result_notStatic2.accessToken(), result_sharedCache1.accessToken());
        assertNotEquals(result_notStatic2.accessToken(), result_sharedCache2.accessToken());

        //Because the confidential client flow has an internal silent call:
        //  -result_notStatic1 and result_notStatic2 should be the same, because they both used the non-static cache from one ConfidentialClientApplication instance
        //  -result_sharedCache1 and result_sharedCache2 should be the same, because they both used the static cache shared between two ConfidentialClientApplication instances
        assertEquals(result_notStatic1.accessToken(), result_notStatic2.accessToken());
        assertEquals(result_sharedCache1.accessToken(), result_sharedCache2.accessToken());
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
