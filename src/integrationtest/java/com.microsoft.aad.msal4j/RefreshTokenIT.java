package com.microsoft.aad.msal4j;

import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutionException;


@Test()
public class RefreshTokenIT {

    private final static Logger LOG = LoggerFactory.getLogger(RefreshTokenIT.class);

    private LabUserProvider labUserProvider;
    private String refreshToken;
    private PublicClientApplication pca;

    @BeforeTest
    public void setUp() throws Exception {
        labUserProvider = new LabUserProvider();
        LabResponse labResponse = labUserProvider.getDefaultUser(false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                TestConstants.GRAPH_DEFAULT_SCOPE,
                labResponse.getUser().getUpn(),
                password).get();

        refreshToken = result.getRefreshToken();
    }

    @Test
    public void acquireTokenWithRefreshToken() throws Exception{
        AuthenticationResult result = pca.acquireTokenByRefreshToken(
                refreshToken,
                TestConstants.GRAPH_DEFAULT_SCOPE).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
    }

    // todo Update test once we start bubbling up exceptions
    @Test(expectedExceptions = ExecutionException.class)
    public void acquireTokenWithRefreshToken_WrongScopes() throws Exception{
        AuthenticationResult result = pca.acquireTokenByRefreshToken(
                refreshToken,
                TestConstants.KEYVAULT_DEFAULT_SCOPE).get();
    }
}
