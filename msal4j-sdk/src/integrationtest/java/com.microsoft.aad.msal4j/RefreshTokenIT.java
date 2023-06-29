// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabUserProvider;
import labapi.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RefreshTokenIT {
    private String refreshToken;
    private PublicClientApplication pca;

    private Config cfg;

    private void setUp(String environment) throws Exception {
        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        User user = labUserProvider.getDefaultUser(environment);

        pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        AuthenticationResult result = (AuthenticationResult) pca.acquireToken(UserNamePasswordParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        refreshToken = result.refreshToken();
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithRefreshToken(String environment) throws Exception {
        cfg = new Config(environment);

        setUp(environment);

        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(cfg.graphDefaultScope()),
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
