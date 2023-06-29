// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UsernamePasswordIT {
    private LabUserProvider labUserProvider;

    private Config cfg;

    @BeforeAll
    void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithUsernamePassword_Managed(String environment) throws Exception {
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope(), user.getAppId());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithUsernamePassword_ADFSv2019_Federated(String environment) throws Exception {
        cfg = new Config(environment);

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, cfg.azureEnvironment);
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.ADFS_2019);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);

        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope(), user.getAppId());
    }

    @Test
    void acquireTokenWithUsernamePassword_ADFSv2019_OnPrem() throws Exception {
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.ADFS_2019);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.ON_PREM);

        User user = labUserProvider.getLabUser(query);

        assertAcquireTokenCommon(user, TestConstants.ADFS_AUTHORITY, TestConstants.ADFS_SCOPE, TestConstants.ADFS_APP_ID);
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithUsernamePassword_ADFSv4(String environment) throws Exception {
        cfg = new Config(environment);

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, cfg.azureEnvironment);
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.ADFS_4);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);

        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope(), user.getAppId());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithUsernamePassword_ADFSv3(String environment) throws Exception {
        cfg = new Config(environment);

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, cfg.azureEnvironment);
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.ADFS_3);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);

        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope(), user.getAppId());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithUsernamePassword_ADFSv2(String environment) throws Exception {
        cfg = new Config(environment);

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, cfg.azureEnvironment);
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER, FederationProvider.ADFS_2);
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);

        assertAcquireTokenCommonAAD(user);
    }

    @Test
    void acquireTokenWithUsernamePassword_Ciam() throws Exception {

        Map<String, String> extraQueryParameters = new HashMap<>();
        extraQueryParameters.put("dc","ESTS-PUB-EUS-AZ1-FD000-TEST1");

        User user = labUserProvider.getCiamUser();
        PublicClientApplication pca = PublicClientApplication.builder(user.getAppId())
                        .authority("https://" + user.getLabName() + ".ciamlogin.com/")
                                .build();


        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                        builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                       .extraQueryParameters(extraQueryParameters)
                        .build())
                .get();

        assertNotNull(result.accessToken());
    }

    @Test
    void acquireTokenWithUsernamePassword_AuthorityWithPort() throws Exception {
        User user = labUserProvider.getDefaultUser();

        assertAcquireTokenCommon(
                user,
                TestConstants.COMMON_AUTHORITY_WITH_PORT,
                TestConstants.GRAPH_DEFAULT_SCOPE,
                user.getAppId());
    }


    private void assertAcquireTokenCommonAAD(User user) throws Exception {
        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope(),
                user.getAppId());
    }

    private void assertAcquireTokenCommon(User user, String authority, String scope, String appId)
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

        assertTokenResultNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    @Test
    void acquireTokenWithUsernamePassword_B2C_CustomAuthority() throws Exception {
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, B2CProvider.LOCAL);
        User user = labUserProvider.getLabUser(query);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                b2cAuthority(TestConstants.B2C_AUTHORITY_ROPC).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.B2C_READ_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        assertTokenResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account);

        result = pca.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account)
                        .build())
                .get();

        assertTokenResultNotNull(result);
    }

    @Test
    void acquireTokenWithUsernamePassword_B2C_LoginMicrosoftOnline() throws Exception {
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, B2CProvider.LOCAL);
        User user = labUserProvider.getLabUser(query);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                b2cAuthority(TestConstants.B2C_MICROSOFTLOGIN_ROPC).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.B2C_READ_SCOPE),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        assertTokenResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account);

        result = pca.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(TestConstants.B2C_READ_SCOPE), account)
                        .build())
                .get();

        assertTokenResultNotNull(result);
    }

    private void assertTokenResultNotNull(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());
    }
}
