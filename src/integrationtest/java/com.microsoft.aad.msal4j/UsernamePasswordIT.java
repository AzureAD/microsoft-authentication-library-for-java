// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import lapapi.FederationProvider;
import lapapi.LabResponse;
import lapapi.LabUserProvider;
import lapapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

@Test()
public class UsernamePasswordIT {

    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = new LabUserProvider();
    }

    @Test
    public void acquireTokenWithUsernamePassword_Managed() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
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
    public void acquireTokenWithUsernamePassword_ADFSv2019() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSv2019,
                true,
                true);
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
    public void acquireTokenWithUsernamePassword_ADFSv4() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
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
    public void acquireTokenWithUsernamePassword_ADFSv3() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV3,
                true,
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
    public void acquireTokenWithUsernamePassword_ADFSv2() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV2,
                true,
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

    public AuthenticationResult acquireTokenCommon(LabResponse labResponse, String password)
            throws Exception{
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
