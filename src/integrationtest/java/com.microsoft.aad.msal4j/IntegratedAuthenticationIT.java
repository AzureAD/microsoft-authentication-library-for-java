package com.microsoft.aad.msal4j;

import lapapi.FederationProvider;
import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.concurrent.ExecutionException;

@Test(groups="integration-tests")
public class IntegratedAuthenticationIT {

    private LabUserProvider labUserProvider;
    private static final String authority = "https://login.microsoftonline.com/organizations/";
    private static final String scopes = "https://graph.windows.net/.default";

    @BeforeClass
    public void setUp() {
        labUserProvider = new LabUserProvider();
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication() throws MalformedURLException,
            InterruptedException, ExecutionException {

        LabResponse labResponse = labUserProvider.getAdfsUser(FederationProvider.ADFSV3,true);
        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();
        AuthenticationResult result = pca.acquireTokenByKerberosAuth(
                scopes,
                labResponse.getUser().getUpn()).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

}
