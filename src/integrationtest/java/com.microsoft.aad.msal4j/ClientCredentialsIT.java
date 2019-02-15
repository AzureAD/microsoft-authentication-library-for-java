package com.microsoft.aad.msal4j;

import lapapi.LabResponse;
import lapapi.LabUserProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "integration-tests")
public class ClientCredentialsIT {

    private LabUserProvider labUserProvider;
    private static final String authority = "https://login.microsoftonline.com/organizations/";
    private static final String scopes = "https://graph.windows.net/.default";

    @BeforeClass
    private void setup(){
        labUserProvider = new LabUserProvider();
    }

    @Test
    public void acquireTokenClientCredentials_ClientSecret() throws Exception{
        LabResponse labResponse = labUserProvider.getDefaultUser();
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        IClientCredential clientCredential = ClientCredentialFactory.create(password);


        ConfidentialClientApplication cca = new ConfidentialClientApplication.Builder(
                labResponse.getAppId(),
                clientCredential).authority(authority).
                build();

        AuthenticationResult result = cca.acquireTokenForClient(scopes).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getRefreshToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());

    }

    @Test
    public void acquireTokenClientCredentials_AsymetricKeyCredential(){

    }

    @Test
    public void acquireTokenClientCredentials_ClientAssertion(){

    }

}
