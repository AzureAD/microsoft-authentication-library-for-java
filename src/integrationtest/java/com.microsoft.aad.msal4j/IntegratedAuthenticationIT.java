package com.microsoft.aad.msal4j;

import lapapi.FederationProvider;
import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test()
public class IntegratedAuthenticationIT {
    private final static Logger LOG = LoggerFactory.getLogger(IntegratedAuthenticationIT.class);

    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = new LabUserProvider();
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv2019() {

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSv2019,
                true,
                true);

        AuthenticationResult result = acquireTokenWithIntegratedWindowsAuthentication(labResponse);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv4() {

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);

        AuthenticationResult result = acquireTokenWithIntegratedWindowsAuthentication(labResponse);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv3() {

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV3,
                true,
                false);

        AuthenticationResult result = acquireTokenWithIntegratedWindowsAuthentication(labResponse);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithIntegratedWindowsAuthentication_ADFSv2() {

        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV2,
                true,
                false);

        AuthenticationResult result = acquireTokenWithIntegratedWindowsAuthentication(labResponse);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    private AuthenticationResult acquireTokenWithIntegratedWindowsAuthentication(
            LabResponse labResponse){
        AuthenticationResult result;
        try{
            PublicClientApplication pca = new PublicClientApplication.Builder(
                    labResponse.getAppId()).
                    authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                    build();
            result = pca.acquireTokenByKerberosAuth(
                    TestConstants.GRAPH_DEFAULT_SCOPE,
                    labResponse.getUser().getUpn()).get();
        } catch(Exception e){
            LOG.error("Error acquiring token: " + e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }
}
