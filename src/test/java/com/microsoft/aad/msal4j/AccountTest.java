// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.testng.Assert;
import org.testng.annotations.Test;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.Map;

public class AccountTest {

    @Test
    public void testMultiTenantAccount_ClassTypes() throws IOException, URISyntaxException {

        ITokenCacheAccessAspect accountCache = new CachePersistenceIT.TokenPersistence(
                TestHelper.readResource(this.getClass(),
                        "/cache_data/multi-tenant-account-cache.json"));

        PublicClientApplication app = PublicClientApplication.builder("uid1")
                .setTokenCacheAccessAspect(accountCache).build();

        Assert.assertEquals(app.getAccounts().join().size(), 3);
        Iterator<IAccount> acctIterator = app.getAccounts().join().iterator();

        IAccount curAccount;
        IAccount account = null, multiAccount = null, tenantProfileAccount = null;
        while (acctIterator.hasNext()) {
            curAccount = acctIterator.next();
            switch (curAccount.username()) {
                case "Account":
                    account = curAccount;
                    break;
                case "MultiAccount":
                    multiAccount = curAccount;
                    break;
                case "TenantProfileNoHome":
                    tenantProfileAccount = curAccount;
                    break;
            }
        }

        Assert.assertTrue(account instanceof Account);
        Assert.assertTrue(multiAccount instanceof MultiTenantAccount);
        Assert.assertTrue(tenantProfileAccount instanceof TenantProfile);
    }

    @Test
    public void testMultiTenantAccount_AccessTenantProfile() throws IOException, URISyntaxException {

        ITokenCacheAccessAspect accountCache = new CachePersistenceIT.TokenPersistence(
                TestHelper.readResource(this.getClass(),
                        "/cache_data/multi-tenant-account-cache.json"));

        PublicClientApplication app = PublicClientApplication.builder("uid1")
                .setTokenCacheAccessAspect(accountCache).build();

        Assert.assertEquals(app.getAccounts().join().size(), 3);
        Iterator<IAccount> acctIterator = app.getAccounts().join().iterator();

        IAccount curAccount;
        while (acctIterator.hasNext()) {
            curAccount = acctIterator.next();

            if (curAccount.username().equals("MultiAccount")) {
                Assert.assertTrue(curAccount instanceof MultiTenantAccount);
                Map<String, ITenantProfile> tenantProfiles = ((MultiTenantAccount) curAccount).getTenantProfiles();
                Assert.assertNotNull(tenantProfiles);
                Assert.assertNotNull(tenantProfiles.get("tenantprofile1"));
                Assert.assertEquals(tenantProfiles.get("tenantprofile1").username(), "TenantProfile");
            }
        }
    }
}
