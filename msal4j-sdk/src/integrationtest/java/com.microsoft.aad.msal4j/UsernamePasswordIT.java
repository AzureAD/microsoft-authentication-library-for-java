// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsernamePasswordIT {
    @Test
    void acquireTokenWithUsernamePassword_Managed() throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        assertAcquireTokenCommon(labResponse.getUser(), TestConstants.ORGANIZATIONS_AUTHORITY, TestConstants.GRAPH_DEFAULT_SCOPE, labResponse.getApp().getAppId());
    }

    @Test
    void acquireTokenWithUsernamePassword_AuthorityWithPort() throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        assertAcquireTokenCommon(
                user,
                TestConstants.MICROSOFT_AUTHORITY_HOST_WITH_PORT + user.getTenantId(),
                TestConstants.GRAPH_DEFAULT_SCOPE,
                labResponse.getApp().getAppId());
    }

    @Test
    void acquireTokenWithUsernamePassword_Ciam() throws Exception {
        Map<String, String> extraQueryParameters = new HashMap<>();

        LabResponse labResponse = LabUserHelper.getCiamCudUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(user.getAppId())
                .authority("https://" + user.getLabName() + ".ciamlogin.com/")
                .build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                        builder(Collections.singleton(TestConstants.USER_READ_SCOPE),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .extraQueryParameters(extraQueryParameters)
                        .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

    private void assertAcquireTokenCommon(LabUser user, String authority, String scope, String appId)
            throws Exception {

        PublicClientApplication pca = PublicClientApplication.builder(
                appId).
                authority(authority).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(scope),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    @Test
    void acquireTokenWithUsernamePassword_B2C_CustomAuthority() throws Exception {
        LabResponse labResponse = LabUserHelper.getB2CLocalAccount();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                b2cAuthority(TestConstants.B2C_AUTHORITY_ROPC).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.B2C_READ_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account);

        result = pca.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account)
                        .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

    @Test
    void acquireTokenWithUsernamePassword_B2C_LoginMicrosoftOnline() throws Exception {
        LabResponse labResponse = LabUserHelper.getB2CLocalAccount();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                b2cAuthority(TestConstants.B2C_MICROSOFTLOGIN_ROPC).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.B2C_READ_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account);

        result = pca.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account)
                        .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }
}
