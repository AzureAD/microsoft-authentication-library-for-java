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

import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.ExecutionException;


@Test()
public class RefreshTokenIT {
    private String refreshToken;
    private PublicClientApplication pca;

    @BeforeTest
    public void setUp() throws Exception {
        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        AuthenticationResult result = (AuthenticationResult)pca.acquireToken(UserNamePasswordParameters
                        .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                labResponse.getUser().getUpn(),
                                password.toCharArray())
                        .build())
                .get();

        refreshToken = result.refreshToken();
    }

    @Test
    public void acquireTokenWithRefreshToken() throws Exception{

        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        refreshToken)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    @Test(expectedExceptions = ExecutionException.class)
    public void acquireTokenWithRefreshToken_WrongScopes() throws Exception{
        IAuthenticationResult result = pca.acquireToken(RefreshTokenParameters
                .builder(
                        Collections.singleton(TestConstants.KEYVAULT_DEFAULT_SCOPE),
                        refreshToken)
                .build())
                .get();
    }
}
