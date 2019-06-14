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

import labapi.FederationProvider;
import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;

public class AcquireTokenSilentIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenSilent_OrganizationAuthority_TokenRefreshed() throws Exception {

        // When using common, organization, or consumer tenants, cache has no way
        // of determining which access token to return therefore token is always refreshed
        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                account).build();

        IAuthenticationResult result =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    @Test
    public void acquireTokenSilent_LabAuthority_TokenNotRefreshed() throws Exception {
        // Access token should be returned from cache, and not using refresh token

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        String labAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + labResponse.getUser().getTenantId();

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(labAuthority).
                build();

        IAuthenticationResult result =  pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), account).
                build();

        IAuthenticationResult acquireSilentResult =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(acquireSilentResult.accessToken());
        Assert.assertNotNull(result.idToken());
        // Check that access and id tokens are coming from cache
        Assert.assertEquals(result.accessToken(), acquireSilentResult.accessToken());
        Assert.assertEquals(result.idToken(), acquireSilentResult.idToken());
    }

    @Test
    public void acquireTokenSilent_ForceRefresh() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        IAuthenticationResult result =  pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), account).
                forceRefresh(true).
                build();

        IAuthenticationResult resultAfterRefresh =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(resultAfterRefresh);
        Assert.assertNotNull(resultAfterRefresh.accessToken());
        Assert.assertNotNull(resultAfterRefresh.idToken());
        // Check that new refresh and id tokens are being returned
        Assert.assertNotEquals(result.accessToken(), resultAfterRefresh.accessToken());
        Assert.assertNotEquals(result.idToken(), resultAfterRefresh.idToken());
    }

    @Test
    public void acquireTokenSilent_MultipleAccountsInCache_UseCorrectAccount() throws Exception {

        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        // get lab user for different account
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        // acquire token for different account
        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount account = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        labResponse.getUser().getUpn())).findFirst().orElse(null);

        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), account).
                forceRefresh(true).
                build();

        IAuthenticationResult result =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(result.account().username(), labResponse.getUser().getUpn());
    }

    private IPublicClientApplication getPublicClientApplicationWithTokensInCache()
            throws Exception {
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = new PublicClientApplication.Builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();
        return pca;
    }
}
