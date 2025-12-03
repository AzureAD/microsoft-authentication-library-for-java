// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenIT {
    private String refreshToken;
    private PublicClientApplication pca;

    private void setUp() throws Exception {
        LabResponse labResponse = LabConfigHelper.getDefaultConfig();
        UserConfig user = labResponse.getUser();

        pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        AuthenticationResult result = (AuthenticationResult) pca.acquireToken(UserNamePasswordParameters
                .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        refreshToken = result.refreshToken();
    }

    @Test
    void acquireTokenWithRefreshToken() throws Exception {
        setUp();

        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        refreshToken)
                .build())
                .get();

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());
    }

    @Test
    void acquireTokenWithRefreshToken_WrongScopes() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> pca.acquireToken(RefreshTokenParameters
                        .builder(
                                Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE),
                                refreshToken)
                        .build())
                .get());
    }
}
