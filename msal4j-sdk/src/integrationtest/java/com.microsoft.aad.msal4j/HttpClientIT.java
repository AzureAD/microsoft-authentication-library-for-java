// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpClientIT {

    @Test
    void acquireToken_okHttpClient() throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();
        assertAcquireTokenCommon(user, labResponse.getApp().getAppId(), new OkHttpClientAdapter());
    }

    @Test
    void acquireToken_apacheHttpClient() throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();
        assertAcquireTokenCommon(user, labResponse.getApp().getAppId(), new ApacheHttpClientAdapter());
    }

    @Test
    void acquireToken_readTimeout() throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        //Set a 1ms read timeout, which will almost certainly occur before the service can respond
        assertAcquireTokenCommon_WithTimeout(user, labResponse.getApp().getAppId(), 1);
    }

    private void assertAcquireTokenCommon(LabUser user, String appId, IHttpClient httpClient)
            throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(
                appId).
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

    private void assertAcquireTokenCommon_WithTimeout(LabUser user, String appId, int readTimeout)
            throws Exception {
        PublicClientApplication pca = PublicClientApplication.builder(
                        appId).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                readTimeoutForDefaultHttpClient(readTimeout).
                build();

        ExecutionException ex = assertThrows(ExecutionException.class, () -> pca.acquireToken(UserNamePasswordParameters.
                        builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .build())
                .get());

        //The timeout may occur during either the connection attempt (SocketTimeoutException) or the SSL handshake (SSLException)
        assertTrue(ex.getMessage().equals("com.microsoft.aad.msal4j.MsalClientException: java.net.SocketTimeoutException: Read timed out") ||
                ex.getMessage().equals("com.microsoft.aad.msal4j.MsalClientException: javax.net.ssl.SSLException: Read timed out"));
    }
}
