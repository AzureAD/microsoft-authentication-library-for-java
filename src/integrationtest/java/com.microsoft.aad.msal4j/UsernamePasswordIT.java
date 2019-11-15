// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.B2CIdentityProvider;
import labapi.FederationProvider;
import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

@Test()
public class UsernamePasswordIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenWithUsernamePassword_Managed() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        assertAcquireTokenCommon(labResponse, password);
    }

    @Test
    public void acquireTokenWithUsernamePassword_ADFSv2019() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSv2019,
                true,
                true);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        assertAcquireTokenCommon(labResponse, password);
    }

    @Test
    public void acquireTokenWithUsernamePassword_ADFSv4() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        assertAcquireTokenCommon(labResponse, password);
    }

    @Test
    public void acquireTokenWithUsernamePassword_ADFSv3() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV3,
                true,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        assertAcquireTokenCommon(labResponse, password);
    }

    @Test
    public void acquireTokenWithUsernamePassword_ADFSv2() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV2,
                true,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        assertAcquireTokenCommon(labResponse, password);
    }

    private void assertAcquireTokenCommon(LabResponse labResponse, String password)
            throws Exception{
        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithUsernamePassword_B2C_CustomAuthority() throws Exception{
        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.LOCAL,
                false);

        String b2CAppId = "e3b9ad76-9763-4827-b088-80c7a7888f79";
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                b2CAppId).
                b2cAuthority(TestConstants.B2C_AUTHORITY_ROPC).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.B2C_READ_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    @Test
    public void acquireTokenWithUsernamePassword_B2C_LoginMicrosoftOnline() throws Exception{
        LabResponse labResponse = labUserProvider.getB2cUser(
                B2CIdentityProvider.LOCAL,
                false);

        String b2CAppId = "e3b9ad76-9763-4827-b088-80c7a7888f79";
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                b2CAppId).
                b2cAuthority(TestConstants.B2C_MICROSOFTLOGIN_ROPC).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.B2C_READ_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }
}
