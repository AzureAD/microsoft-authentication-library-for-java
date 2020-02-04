// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabUserProvider;
import labapi.AzureEnvironment;
import labapi.User;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;

public class HttpClientIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireToken_okHttpClient() throws Exception {
        User user = labUserProvider.getDefaultUser();
        assertAcquireTokenCommon(user, new OkHttpClientAdapter());
    }

    @Test
    public void acquireToken_apacheHttpClient() throws Exception {
        User user = labUserProvider.getDefaultUser();
        assertAcquireTokenCommon(user, new ApacheHttpClientAdapter());
    }

    private void assertAcquireTokenCommon(User user, IHttpClient httpClient)
            throws Exception{
        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                httpClient(httpClient).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }
}
