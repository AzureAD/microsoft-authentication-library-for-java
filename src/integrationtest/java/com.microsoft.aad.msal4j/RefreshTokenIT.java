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

    @BeforeTest
    public void setUp() throws Exception {
        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        User user = labUserProvider.getDefaultUser();

        pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        AuthenticationResult result = (AuthenticationResult)pca.acquireToken(UserNamePasswordParameters
                        .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .build())
                .get();

        refreshToken = result.refreshToken();
    }

    @Test
    public void acquireTokenWithRefreshToken() throws Exception{

        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
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
