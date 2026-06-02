// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.UserConfig;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class IntegrationTestHelper {

    // --- Application builders ---

    static PublicClientApplication createPublicApp(String appId, String authority) {
        try {
            return PublicClientApplication.builder(appId)
                    .authority(authority)
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    static ConfidentialClientApplication createCca(String clientId, IClientCredential credential, String authority) {
        try {
            return ConfidentialClientApplication.builder(clientId, credential)
                    .authority(authority)
                    .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    // --- Token acquisition shortcuts ---

    static IAuthenticationResult acquireTokenByRopc(
            PublicClientApplication pca, UserConfig user, Set<String> scopes)
            throws ExecutionException, InterruptedException {
        return pca.acquireToken(UserNamePasswordParameters
                .builder(scopes, user.getUpn(), user.getPassword().toCharArray())
                .build())
                .get();
    }

    static IAuthenticationResult acquireTokenByRopc(
            PublicClientApplication pca, UserConfig user, String... scopes)
            throws ExecutionException, InterruptedException {
        return acquireTokenByRopc(pca, user, toScopeSet(scopes));
    }

    static IAuthenticationResult acquireTokenSilently(
            IPublicClientApplication pca, IAccount account, Set<String> scopes)
            throws ExecutionException, InterruptedException, MalformedURLException {
        return pca.acquireTokenSilently(SilentParameters
                .builder(scopes, account)
                .build())
                .get();
    }

    static IAuthenticationResult acquireTokenSilently(
            IPublicClientApplication pca, IAccount account, String... scopes)
            throws ExecutionException, InterruptedException, MalformedURLException {
        return acquireTokenSilently(pca, account, toScopeSet(scopes));
    }

    // --- Assertions ---

    static void assertAccessAndIdTokensNotNull(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());
    }

    static void assertAccessTokenNotNull(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
    }

    static void assertAccessTokensEqual(IAuthenticationResult result1, IAuthenticationResult result2) {
        assertEquals(result1.accessToken(), result2.accessToken());
    }

    static void assertAccessTokensNotEqual(IAuthenticationResult result1, IAuthenticationResult result2) {
        assertNotEquals(result1.accessToken(), result2.accessToken());
    }

    static void assertTokensAreEqual(IAuthenticationResult result1, IAuthenticationResult result2) {
        assertEquals(result1.accessToken(), result2.accessToken());
        assertEquals(result1.idToken(), result2.idToken());
    }

    static void assertTokensAreNotEqual(IAuthenticationResult result1, IAuthenticationResult result2) {
        assertNotEquals(result1.accessToken(), result2.accessToken());
        assertNotEquals(result1.idToken(), result2.idToken());
    }

    // --- Utilities ---

    private static Set<String> toScopeSet(String... scopes) {
        if (scopes.length == 1) {
            return Collections.singleton(scopes[0]);
        }
        return new HashSet<>(Arrays.asList(scopes));
    }
}
