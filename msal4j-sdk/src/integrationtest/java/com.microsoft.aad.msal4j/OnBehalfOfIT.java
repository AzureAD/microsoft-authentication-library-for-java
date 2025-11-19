// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OnBehalfOfIT {

    private Config cfg;

    @Test
    void acquireTokenWithOBO_Managed() throws Exception {
        cfg = new Config();
        String accessToken = this.getAccessToken();

        final String clientId = cfg.appProvider().getOboAppId();
        final String password = cfg.appProvider().getOboAppPassword();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(password)).
                        authority(cfg.tenantSpecificAuthority()).
                        build();

        IAuthenticationResult result =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        new UserAssertion(accessToken)).build()).
                        get();

        assertResultNotNull(result);
    }

    @Test
    void acquireTokenWithOBO_testCache() throws Exception {
        String accessToken = this.getAccessToken();

        final String clientId = cfg.appProvider().getOboAppId();
        final String password = cfg.appProvider().getOboAppPassword();

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder(clientId, ClientCredentialFactory.createFromSecret(password)).
                        authority(cfg.tenantSpecificAuthority()).
                        build();

        IAuthenticationResult result1 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.USER_READ_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        assertResultNotNull(result1);

        // Same scope and userAssertion, should return cached tokens
        IAuthenticationResult result2 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(TestConstants.USER_READ_SCOPE),
                        new UserAssertion(accessToken)).build()).
                        get();

        assertEquals(result1.accessToken(), result2.accessToken());

        // Scope 2, should return new token
        IAuthenticationResult result3 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        new UserAssertion(accessToken)).build()).
                        get();

        assertResultNotNull(result3);
        assertNotEquals(result2.accessToken(), result3.accessToken());

        // Scope 2, should return cached token
        IAuthenticationResult result4 =
                cca.acquireToken(OnBehalfOfParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        new UserAssertion(accessToken)).build()).
                        get();

        assertEquals(result3.accessToken(), result4.accessToken());

        // skipCache=true, should return new token
        IAuthenticationResult result5 =
                cca.acquireToken(
                        OnBehalfOfParameters.builder(
                                Collections.singleton(cfg.graphDefaultScope()),
                                new UserAssertion(accessToken))
                                .skipCache(true)
                                .build()).
                        get();

        assertResultNotNull(result5);
        assertNotEquals(result5.accessToken(), result4.accessToken());
        assertNotEquals(result5.accessToken(), result2.accessToken());


        String newAccessToken = this.getAccessToken();
        // New new UserAssertion, should return new token
        IAuthenticationResult result6 =
                cca.acquireToken(
                        OnBehalfOfParameters.builder(
                                Collections.singleton(cfg.graphDefaultScope()),
                                new UserAssertion(newAccessToken))
                                .build()).
                        get();

        assertResultNotNull(result6);
        assertNotEquals(result6.accessToken(), result5.accessToken());
        assertNotEquals(result6.accessToken(), result4.accessToken());
        assertNotEquals(result6.accessToken(), result2.accessToken());
    }

    private void assertResultNotNull(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
    }

    private String getAccessToken() throws Exception {

        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        String clientId = cfg.appProvider().getAppId();
        String apiReadScope = cfg.appProvider().getOboAppIdURI() + "/user_impersonation";
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