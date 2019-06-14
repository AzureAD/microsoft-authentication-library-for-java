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

import labapi.FederationProvider;
import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
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
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        // Check that cache is empty
        Assert.assertEquals(pca.getAccounts().join().size() , 0);

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
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

        LabResponse labResponse1 = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse1.getUser());

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse1.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        Assert.assertEquals(pca.getAccounts().join().size() , 0);

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse1.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Assert.assertEquals(pca.getAccounts().join().size() , 1);

        // get lab user for different account
        LabResponse labResponse2 = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);
        String password2 = labUserProvider.getUserPassword(labResponse1.getUser());

        // acquire token for different account
        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse2.getUser().getUpn(),
                        password2.toCharArray())
                .build())
                .get();

        Assert.assertEquals(pca.getAccounts().join().size(), 2);

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount accountLabResponse1 = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        labResponse1.getUser().getUpn())).findFirst().orElse(null);

        pca.removeAccount(accountLabResponse1).join();

        Assert.assertEquals(pca.getAccounts().join().size() , 1);

        IAccount accountLabResponse2 = pca.getAccounts().get().iterator().next();

        // Check that the right account was left in the cache
        Assert.assertEquals(accountLabResponse2.username(), labResponse2.getUser().getUpn());
    }

    @Test
    public void twoAccountsInCache_SameUserDifferentTenants_RemoveAccountTest() throws Exception{
        LabResponse labResponse = labUserProvider.getExternalUser(false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        String dataToInitCache = TestHelper.readResource(
                this.getClass(),
                "/cache_data/remove-account-test-cache.json");

        // check that cache is empty
        Assert.assertEquals(dataToInitCache, "");

        ITokenCacheAccessAspect persistenceAspect = new TokenPersistence(dataToInitCache);

        // acquire tokens for home tenant, and serialize cache
        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY)
                .setTokenCacheAccessAspect(persistenceAspect)
                .build();

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getHomeUpn(),
                        password.toCharArray())
                .build())
                .get();

        String guestTenantAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + labResponse.getUser().getTenantId();

        // initialize pca with tenant where user is guest, deserialize cache, and acquire second token
        PublicClientApplication pca2 = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(guestTenantAuthority).
                setTokenCacheAccessAspect(persistenceAspect).
                build();

        pca2.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getHomeUpn(),
                        password.toCharArray())
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
