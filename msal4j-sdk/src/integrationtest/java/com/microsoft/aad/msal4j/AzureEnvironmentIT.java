// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import static com.microsoft.aad.msal4j.labapi.KeyVaultSecrets.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AzureEnvironmentIT {

    @Test
    void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(APP_ARLINGTON);
        UserConfig user = LabResponseHelper.getUserConfig(USER_ARLINGTON);

        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(
                app.getAppId(), app.getAuthority() + "organizations/");

        IAuthenticationResult result = IntegrationTestHelper.acquireTokenByRopc(
                pca, user, TestConstants.USER_READ_SCOPE);

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());    }
}
