// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AzureEnvironmentIT {
    private LabUserProvider labUserProvider;

    @BeforeAll
    void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    void acquireTokenWithUsernamePassword_AzureChina() throws Exception {
        assertAcquireTokenCommon(AzureEnvironment.AZURE_CHINA);
    }

    @Test
    void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
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

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());

        assertEquals(user.getUpn(), result.account().username());
    }
}
