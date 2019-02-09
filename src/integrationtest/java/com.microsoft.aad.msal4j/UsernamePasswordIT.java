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
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

@Test(groups = "integration-tests")
public class UsernamePasswordIT {

    private LabUserProvider labUserProvider;
    private static final String authority = "https://login.microsoftonline.com/organizations/";
    private static final String scopes = "https://graph.windows.net/.default";

    @BeforeClass
    public void setUp() {
        labUserProvider = new LabUserProvider();
    }

    @Test
    public void acquireTokenWithManagedUsernamePasswordAsync() throws MalformedURLException,
            InterruptedException, ExecutionException {

        LabResponse labResponse = labUserProvider.getDefaultUser();
        char[] password = labUserProvider.getUserPassword(labResponse.getUser()).toCharArray();
        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                scopes,
                labResponse.getUser().getUpn(),
                password.toString()).get();

        Arrays.fill(password, '*');

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getIdToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }

    @Test
    public void acquireTokenWithFederatedUsernamePasswordAsync() throws MalformedURLException,
            InterruptedException, ExecutionException{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true);
        char[] password = labUserProvider.getUserPassword(labResponse.getUser()).toCharArray();

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();

        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                scopes,
                labResponse.getUser().getUpn(),
                password.toString()).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getIdToken());
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void AcquireTokenWithManagedUsernameIncorrectPassword() throws MalformedURLException,
            InterruptedException, ExecutionException {
        LabResponse labResponse = labUserProvider.getDefaultUser();
        char[] password = labUserProvider.getUserPassword(labResponse.getUser()).toCharArray();
        password[password.length + 1] = 'x';

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                scopes,
                labResponse.getUser().getUpn(),
                password.toString()).get();
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void AcquireTokenWithFederatedUsernameIncorrectPassword() throws Exception{
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true);
        char[] password = labUserProvider.getUserPassword(labResponse.getUser()).toCharArray();
        password[password.length + 1] = 'x';

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(authority).
                build();
        AuthenticationResult result = pca.acquireTokenByUsernamePassword(
                scopes,
                labResponse.getUser().getUpn(),
                password.toString()).get();
    }

}
