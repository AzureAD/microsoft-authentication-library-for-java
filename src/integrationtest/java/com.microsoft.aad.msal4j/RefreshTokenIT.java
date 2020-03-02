// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabUserProvider;
import labapi.AzureEnvironment;
import labapi.User;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;


@Test()
public class RefreshTokenIT {
    private String refreshToken;
    private PublicClientApplication pca;

    private Config cfg;

    private void setUp() throws Exception {
        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        User user = labUserProvider.getDefaultUser();

        pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        AuthenticationResult result = (AuthenticationResult)pca.acquireToken(UserNamePasswordParameters
                        .builder(Collections.singleton(cfg.graphDefaultScope()),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .build())
                .get();

        refreshToken = result.refreshToken();
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithRefreshToken(String environment) throws Exception{
        cfg = new Config(environment);

        setUp();

        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        refreshToken)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    @Test(expectedExceptions = ExecutionException.class)
    public void acquireTokenWithRefreshToken_WrongScopes() throws Exception{
        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE),
                        refreshToken)
                .build())
                .get();
    }
}
