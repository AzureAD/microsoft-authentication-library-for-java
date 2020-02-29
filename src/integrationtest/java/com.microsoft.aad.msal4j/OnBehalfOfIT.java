// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

@Test
public class OnBehalfOfIT {
    private String accessToken;

    private Config cfg;

    private void setUp() throws Exception{
        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        String clientId = cfg.appProvider.getAppId();
        String apiReadScope = cfg.appProvider.getOboAppIdURI()  + "/user_impersonation";

        PublicClientApplication pca = PublicClientApplication.builder(
                clientId).
                authority(cfg.tenantSpecificAuthority()).
                build();

        IAuthenticationResult result = pca.acquireToken(
                UserNamePasswordParameters.builder(Collections.singleton(apiReadScope),
                        user.getUpn(),
                        user.getPassword().toCharArray()).build()).get();

        accessToken = result.accessToken();
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithOBO_Managed(String environment) throws Exception {
        cfg = new Config(environment);

        setUp();

        final String clientId = cfg.appProvider.getOboAppId();
        final String password = cfg.appProvider.getOboAppPassword();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(password)).
                        authority(cfg.tenantSpecificAuthority()).
                        build();

        IAuthenticationResult result =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        new UserAssertion(accessToken)).build()).
                        get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }
}