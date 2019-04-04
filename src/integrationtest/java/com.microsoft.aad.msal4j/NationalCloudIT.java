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
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGermany() throws Exception {
        assertAcquireTokenCommon(NationalCloud.GERMAN_CLOUD);
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureChina() throws Exception {
        assertAcquireTokenCommon(NationalCloud.CHINA_CLOUD);
    }

    @Test
    public void acquireTokenWithUsernamePassword_AzureGovernment() throws Exception {
        assertAcquireTokenCommon(NationalCloud.GOVERNMENT_CLOUD);
    }

    private void assertAcquireTokenCommon(NationalCloud cloud) throws Exception{
        LabResponse labResponse = labUserProvider.getDefaultUser(
                cloud,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.AUTHORITY_ORGANIZATIONS).
                build();

        AuthenticationResult result = pca.acquireToken(UserNamePasswordParameters
                        .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                labResponse.getUser().getUpn(),
                                password.toCharArray())
                        .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.refreshToken());
        Assert.assertNotNull(result.idToken());
        // TODO AuthenticationResult should have an getAccountInfo API
        // Assert.assertEquals(labResponse.getUser().getUpn(), result.getAccountInfo().getUsername());
    }
}
