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

    // @Test
    // hardcoded token secrets should not be used to not trigger CredScan
    public void testMultiTenantAccount_AccessTenantProfile() throws IOException, URISyntaxException {

        ITokenCacheAccessAspect accountCache = new CachePersistenceIT.TokenPersistence(
                TestHelper.readResource(this.getClass(),
                        "/cache_data/multi-tenant-account-cache.json"));

        PublicClientApplication app = PublicClientApplication.builder("client_id")
                .setTokenCacheAccessAspect(accountCache).build();

        Assert.assertEquals(app.getAccounts().join().size(), 3);
        Iterator<IAccount> acctIterator = app.getAccounts().join().iterator();

        IAccount curAccount;
        while (acctIterator.hasNext()) {
            curAccount = acctIterator.next();

            switch (curAccount.username()) {
                case "MultiTenantAccount": {
                    Assert.assertEquals(curAccount.homeAccountId(), "uid1.utid1");
                    Map<String, ITenantProfile> tenantProfiles = curAccount.getTenantProfiles();
                    Assert.assertNotNull(tenantProfiles);
                    Assert.assertEquals(tenantProfiles.size(), 3);
                    Assert.assertNotNull(tenantProfiles.get("utid1"));
                    Assert.assertNotNull(tenantProfiles.get("utid1").getClaims());
                    Assert.assertNotNull(tenantProfiles.get("utid2"));
                    Assert.assertNotNull(tenantProfiles.get("utid2").getClaims());
                    Assert.assertNotNull(tenantProfiles.get("utid3"));
                    Assert.assertNotNull(tenantProfiles.get("utid3").getClaims());
                    break;
                }
                case "SingleTenantAccount": {
                    Assert.assertEquals(curAccount.homeAccountId(), "uid6.utid5");
                    Map<String, ITenantProfile> tenantProfiles = curAccount.getTenantProfiles();
                    Assert.assertNotNull(tenantProfiles);
                    Assert.assertEquals(tenantProfiles.size(), 1);
                    Assert.assertNotNull(tenantProfiles.get("utid5"));
                    Assert.assertNotNull(tenantProfiles.get("utid5").getClaims());
                    break;
                }
                case "TenantProfileNoHome": {
                    Assert.assertEquals(curAccount.homeAccountId(), "uid5.utid4");
                    Map<String, ITenantProfile> tenantProfiles = curAccount.getTenantProfiles();
                    Assert.assertNotNull(tenantProfiles);
                    Assert.assertEquals(tenantProfiles.size(), 1);
                    Assert.assertNotNull(tenantProfiles.get("utid4"));
                    Assert.assertNotNull(tenantProfiles.get("utid4").getClaims());
                    break;
                }
            }
        }
    }
}
