// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
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
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        AuthenticationResult result = (AuthenticationResult)pca.acquireToken(UserNamePasswordParameters
                        .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                labResponse.getUser().getUpn(),
                                password.toCharArray())
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
