// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

public class NationalCloudIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGermany() throws Exception {
        assertAcquireTokenCommon(NationalCloud.GERMAN_CLOUD);
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureChina() throws Exception {
        assertAcquireTokenCommon(NationalCloud.CHINA_CLOUD);
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
        assertAcquireTokenCommon(NationalCloud.GOVERNMENT_CLOUD);
    }

    private void assertAcquireTokenCommon(NationalCloud cloud) throws Exception{
        LabResponse labResponse = labUserProvider.getDefaultUser(
                cloud,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters
                        .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                labResponse.getUser().getUpn(),
                                password.toCharArray())
                        .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }
}
