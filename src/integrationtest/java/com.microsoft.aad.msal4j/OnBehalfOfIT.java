// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

@Test
public class OnBehalfOfIT {

    private Config cfg;

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithOBO_Managed(String environment) throws Exception {
        cfg = new Config(environment);
        String accessToken = this.getAccessToken();

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
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithOBO_testCache(String environment) throws Exception {
        cfg = new Config(environment);
        String accessToken = this.getAccessToken();

        final String clientId = cfg.appProvider.getOboAppId();
        final String password = cfg.appProvider.getOboAppPassword();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(password)).
                        authority(cfg.tenantSpecificAuthority()).
                        build();

        IAuthenticationResult result1 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.USER_READ_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        Assert.assertNotNull(result1);
        Assert.assertNotNull(result1.accessToken());

        // Same scope and userAssertion, should return cached tokens
        IAuthenticationResult result2 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.USER_READ_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        Assert.assertEquals(result1.accessToken(), result2.accessToken());

        // Scope 2, should return new token
        IAuthenticationResult result3 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        new UserAssertion(accessToken)).build()).
                        get();

        Assert.assertNotNull(result3);
        Assert.assertNotNull(result3.accessToken());
        Assert.assertNotEquals(result2.accessToken(), result3.accessToken());

        // Scope 2, should return cached token
        IAuthenticationResult result4 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        new UserAssertion(accessToken)).build()).
                        get();

        Assert.assertEquals(result3.accessToken(), result4.accessToken());

        // skipCache=true, should return new token
        IAuthenticationResult result5 =
                cca.acquireToken(
                        OnBehalfOfParameters.builder(
                                Collections.singleton(cfg.graphDefaultScope()),
                                new UserAssertion(accessToken))
                                .skipCache(true)
                                .build()).
                        get();

        Assert.assertNotNull(result5);
        Assert.assertNotNull(result5.accessToken());
        Assert.assertNotEquals(result5.accessToken(), result4.accessToken());
        Assert.assertNotEquals(result5.accessToken(), result2.accessToken());


        String newAccessToken = this.getAccessToken();
        // New new UserAssertion, should return new token
        IAuthenticationResult result6 =
                cca.acquireToken(
                        OnBehalfOfParameters.builder(
                                Collections.singleton(cfg.graphDefaultScope()),
                                new UserAssertion(newAccessToken))
                                .build()).
                        get();

        Assert.assertNotNull(result6);
        Assert.assertNotNull(result6.accessToken());
        Assert.assertNotEquals(result6.accessToken(), result5.accessToken());
        Assert.assertNotEquals(result6.accessToken(), result4.accessToken());
        Assert.assertNotEquals(result6.accessToken(), result2.accessToken());
    }

    private String getAccessToken() throws Exception {

        LabUserProvider labUserProvider = LabUserProvider.getInstance();
        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        String clientId = cfg.appProvider.getAppId();
        String apiReadScope = cfg.appProvider.getOboAppIdURI() + "/user_impersonation";
        PublicClientApplication pca = PublicClientApplication.builder(
                clientId).
                authority(cfg.tenantSpecificAuthority()).
                build();

        IAuthenticationResult result = pca.acquireToken(
                UserNamePasswordParameters.builder(Collections.singleton(apiReadScope),
                        user.getUpn(),
                        user.getPassword().toCharArray()).build()).get();

        return result.accessToken();
    }
}