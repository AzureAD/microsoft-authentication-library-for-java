// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabUserProvider;
import labapi.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpClientIT {
    private LabUserProvider labUserProvider;

    @BeforeAll
    void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    void acquireToken_okHttpClient() throws Exception {
        User user = labUserProvider.getDefaultUser();
        assertAcquireTokenCommon(user, new OkHttpClientAdapter());
    }

    @Test
    void acquireToken_apacheHttpClient() throws Exception {
        User user = labUserProvider.getDefaultUser();
        assertAcquireTokenCommon(user, new ApacheHttpClientAdapter());
    }

    @Test
    void acquireToken_readTimeout() throws Exception {
        User user = labUserProvider.getDefaultUser();

        //Set a 1ms read timeout, which will almost certainly occur before the service can respond
        assertAcquireTokenCommon_WithTimeout(user, 1);
    }

    private void assertAcquireTokenCommon(User user, IHttpClient httpClient)
            throws Exception {
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

        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());
        assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenCommon_WithTimeout(User user, int readTimeout)
            throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(
                        user.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                readTimeoutForDefaultHttpClient(readTimeout).
                build();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> pca.acquireToken(UserNamePasswordParameters.
                        builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .build())
                .get());

        assertEquals("com.microsoft.aad.msal4j.MsalClientException: java.net.SocketTimeoutException: Read timed out", ex.getMessage());
    }
}
