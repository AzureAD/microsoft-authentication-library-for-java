package com.microsoft.aad.msal4j;

import lapapi.AppIdentityProvider;
import lapapi.LabResponse;
import lapapi.LabUserProvider;
import lapapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

@Test
public class OnBehalfOfIT {

    private String accessToken;
    private String msidlab4Authority = "https://login.microsoftonline.com/msidlab4.onmicrosoft.com/";

    @BeforeClass
    public void setUp() throws Exception{
        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        String clientId = "c0485386-1e9a-4663-bc96-7ab30656de7f";
        String apiReadScope = "api://f4aa5217-e87c-42b2-82af-5624dd14ee72/read";

        PublicClientApplication pca = PublicClientApplication.builder(
                clientId).
                authority(msidlab4Authority).
                build();

        AuthenticationResult result = pca.acquireToken(
                UserNamePasswordParameters.builder(Collections.singleton(apiReadScope),
                        labResponse.getUser().getUpn(),
                        labResponse.getUser().getPassword().toCharArray()).build()).get();
        accessToken = result.accessToken();
    }

    @Test
    public void acquireTokenWithOBO_Managed() throws Exception {
        final String clientId = "f4aa5217-e87c-42b2-82af-5624dd14ee72";

        AppIdentityProvider appProvider = new AppIdentityProvider();
        final String password = appProvider.getOboPassword();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.create(password)).
                        authority(msidlab4Authority).
                        build();

        AuthenticationResult result =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.refreshToken());
        Assert.assertNotNull(result.idToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }
}