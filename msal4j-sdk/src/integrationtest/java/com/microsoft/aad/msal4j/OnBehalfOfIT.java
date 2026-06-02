// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import static com.microsoft.aad.msal4j.labapi.KeyVaultSecrets.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OnBehalfOfIT {

    private IClientCertificate certificate;

    @BeforeAll
    void init() throws Exception {
        certificate = CertificateHelper.getClientCertificate();
    }

    @Test
    void acquireTokenWithOBO_Managed() throws Exception {
        String accessToken = this.getAccessToken();
        AppConfig oboService = LabResponseHelper.getAppConfig(APP_OBO_SERVICE);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(oboService.getAppId(), certificate).
                        authority(TestConstants.MICROSOFT_AUTHORITY_HOST + user.getTenantId()).
                        build();

        IAuthenticationResult result =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        IntegrationTestHelper.assertAccessTokenNotNull(result);
    }

    @Test
    void acquireTokenWithOBO_testCache() throws Exception {
        String accessToken = this.getAccessToken();

        AppConfig oboService = LabResponseHelper.getAppConfig(APP_OBO_SERVICE);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(oboService.getAppId(), certificate).
                        authority(TestConstants.MICROSOFT_AUTHORITY_HOST + user.getTenantId()).
                        build();

        IAuthenticationResult result1 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.USER_READ_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        IntegrationTestHelper.assertAccessTokenNotNull(result1);

        // Same scope and userAssertion, should return cached tokens
        IAuthenticationResult result2 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.USER_READ_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        IntegrationTestHelper.assertAccessTokensEqual(result1, result2);

        // Scope 2, should return new token
        IAuthenticationResult result3 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        IntegrationTestHelper.assertAccessTokenNotNull(result3);
        IntegrationTestHelper.assertAccessTokensNotEqual(result2, result3);

        // Scope 2, should return cached token
        IAuthenticationResult result4 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        IntegrationTestHelper.assertAccessTokensEqual(result3, result4);

        // skipCache=true, should return new token
        IAuthenticationResult result5 =
                cca.acquireToken(
                        OnBehalfOfParameters.builder(
                                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                new UserAssertion(accessToken))
                                .skipCache(true)
                                .build()).
                        get();

        IntegrationTestHelper.assertAccessTokenNotNull(result5);
        IntegrationTestHelper.assertAccessTokensNotEqual(result5, result4);
        IntegrationTestHelper.assertAccessTokensNotEqual(result5, result2);


        String newAccessToken = this.getAccessToken();
        // New new UserAssertion, should return new token
        IAuthenticationResult result6 =
                cca.acquireToken(
                        OnBehalfOfParameters.builder(
                                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                new UserAssertion(newAccessToken))
                                .build()).
                        get();

        IntegrationTestHelper.assertAccessTokenNotNull(result6);
        IntegrationTestHelper.assertAccessTokensNotEqual(result6, result5);
        IntegrationTestHelper.assertAccessTokensNotEqual(result6, result4);
        IntegrationTestHelper.assertAccessTokensNotEqual(result6, result2);
    }

    private String getAccessToken() throws Exception {
        AppConfig oboService = LabResponseHelper.getAppConfig(APP_OBO_SERVICE);
        AppConfig oboClient = LabResponseHelper.getAppConfig(APP_OBO_CLIENT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        String apiReadScope = oboService.getDefaultScopes() != null
                ? oboService.getDefaultScopes()
                : "api://" + oboService.getAppId() + "/access_as_user";

        PublicClientApplication pca = PublicClientApplication.builder(oboClient.getAppId()).
                authority("https://login.microsoftonline.com/organizations").
                build();

        IAuthenticationResult result = pca.acquireToken(
                UserNamePasswordParameters.builder(Collections.singleton(apiReadScope),
                        user.getUpn(),
                        user.getPassword().toCharArray()).build()).get();

        return result.accessToken();
    }
}