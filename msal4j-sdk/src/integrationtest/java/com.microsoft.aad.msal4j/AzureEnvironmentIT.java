// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AzureEnvironmentIT {

    // TODO: labapi2 doesn't have Azure China user configuration yet - will be pulled from MSAL.NET
//    @Test
//    void acquireTokenWithUsernamePassword_AzureChina() throws Exception {
//        assertAcquireTokenCommon(AzureEnvironment.AZURE_CHINA);
//    }

    @Test
    void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
        LabResponse labResponse = LabUserHelper.getArlingtonUser();
        LabUser user = labResponse.getUser();
        LabApp app = labResponse.getApp();

        PublicClientApplication pca = PublicClientApplication.builder(
                        app.getAppId()).
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

        assertEquals(user.getUpn(), result.account().username());    }
}
