// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

public class AzureEnvironmentIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureChina() throws Exception {
        assertAcquireTokenCommon(AzureEnvironment.AZURE_CHINA);
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
        assertAcquireTokenCommon(AzureEnvironment.AZURE_US_GOVERNMENT);
    }

    private void assertAcquireTokenCommon(String azureEnvironment) throws Exception {
        User user = labUserProvider.getUserByAzureEnvironment(azureEnvironment);

        App app = LabService.getApp(user.getAppId());

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(app.getAuthority() + "organizations/").
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters
                .builder(Collections.singleton(TestConstants.USER_READ_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());

        Assert.assertEquals(user.getUpn(), result.account().username());
    }
}
