// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
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

        LabResponse labResponse = getManagedUserAccountWithPassword();
        assertAcquireTokenCommon(labResponse, new OkHttpClientAdapter());
    }

    @Test
    public void acquireToken_apacheHttpClient() throws Exception {

        LabResponse labResponse = getManagedUserAccountWithPassword();
        assertAcquireTokenCommon(labResponse, new ApacheHttpClientAdapter());
    }

    private void assertAcquireTokenCommon(LabResponse labResponse, IHttpClient httpClient)
            throws Exception{
        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                httpClient(httpClient).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        labResponse.getUser().getPassword().toCharArray())
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(labResponse.getUser().getUpn(), result.account().username());
    }

    private LabResponse getManagedUserAccountWithPassword(){
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        return labResponse;
    }
}
