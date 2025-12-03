// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AzureEnvironmentIT {

    @Test
    void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
        LabResponse labResponse = LabConfigHelper.getArlingtonConfig();
        UserConfig user = labResponse.getUser();
        AppConfig app = labResponse.getApp();

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
