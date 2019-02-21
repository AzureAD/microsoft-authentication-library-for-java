package com.microsoft.aad.msal4j;

import lapapi.LabResponse;
import lapapi.LabUserProvider;
import lapapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

public class NationalCloudIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = new LabUserProvider();
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGermany() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.GERMAN_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        AuthenticationResult result = acquireTokenCommon(labResponse, password);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureChina() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.CHINA_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        AuthenticationResult result = acquireTokenCommon(labResponse, password);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.GOVERNMENT_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        AuthenticationResult result = acquireTokenCommon(labResponse, password);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    public AuthenticationResult acquireTokenCommon(
            LabResponse labResponse,
            String password)
            throws Exception {
        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                labResponse.getUser().getUpn(),
                password).
                get();
        return result;
    }
}
