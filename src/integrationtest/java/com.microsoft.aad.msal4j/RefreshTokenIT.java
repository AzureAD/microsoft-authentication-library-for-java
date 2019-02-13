package com.microsoft.aad.msal4j;

import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


@Test(groups = "integration-tests")
public class RefreshTokenIT {

    private final static Logger LOG = LoggerFactory.getLogger(RefreshTokenIT.class);

    private LabUserProvider labUserProvider;
    private static final String authority = "https://login.microsoftonline.com/organizations/";
    private static final String scopes = "https://graph.windows.net/.default";
    //TODO What happens when you try to acquire token with RT from wrong scopes
    private static final String wrongScopes = "";
    private String refreshToken;
    private PublicClientApplication pca;

    @BeforeClass
    public void setUp() throws Exception {
        labUserProvider = new LabUserProvider();
        LabResponse labResponse = labUserProvider.getDefaultUser();
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                scopes,
                labResponse.getUser().getUpn(),
                password).get();

        refreshToken = result.getRefreshToken();
    }

    @Test
    public void acquireTokenWithRefreshToken() throws Exception{
        AuthenticationResult result = pca.acquireTokenByRefreshToken(
                refreshToken,
                scopes).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void acquireTokenWithRefreshToken_WrongScopes() throws Exception {
        AuthenticationResult result = pca.acquireTokenByRefreshToken(
                refreshToken,
                wrongScopes).get();

    }
}
