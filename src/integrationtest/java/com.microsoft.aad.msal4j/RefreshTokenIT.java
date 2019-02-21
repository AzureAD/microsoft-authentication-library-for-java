package com.microsoft.aad.msal4j;

import lapapi.LabResponse;
import lapapi.LabUserProvider;
import lapapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;


@Test()
public class RefreshTokenIT {

    private LabUserProvider labUserProvider;
    private String refreshToken;
    private PublicClientApplication pca;

    @BeforeTest
    public void setUp() throws Exception {
        labUserProvider = new LabUserProvider();
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                labResponse.getUser().getUpn(),
                password).
                get();

        refreshToken = result.getRefreshToken();
    }

    @Test
    public void acquireTokenWithRefreshToken() throws Exception{
        AuthenticationResult result = pca.acquireTokenByRefreshToken(
                refreshToken,
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE)).
                get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
    }

    @Test(expectedExceptions = ExecutionException.class)
    public void acquireTokenWithRefreshToken_WrongScopes() throws Exception{
        AuthenticationResult result = pca.acquireTokenByRefreshToken(
                refreshToken,
                Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE)).
                get();
    }
}
