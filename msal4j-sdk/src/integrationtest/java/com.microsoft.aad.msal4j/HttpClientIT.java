// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.LabUserProvider;
import labapi.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

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
}
