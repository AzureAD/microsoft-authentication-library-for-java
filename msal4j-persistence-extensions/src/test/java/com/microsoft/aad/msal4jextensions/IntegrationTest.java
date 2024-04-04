// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4j.*;
import org.junit.Assert;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class IntegrationTest {

    private PersistenceSettings createPersistenceSettings() throws IOException {

        Path path = Paths.get(System.getProperty("user.home"), "MSAL", "testCache");

        return PersistenceSettings.builder("testCacheFile", path)
                .setMacKeychain("MsalTestService", "MsalTestAccount")
                .setLinuxKeyring(null,
                        "MsalTestSchema",
                        "MsalTestSecretLabel",
                        "MsalTestAttribute1Key",
                        "MsalTestAttribute1Value",
                        "MsalTestAttribute2Key",
                        "MsalTestAttribute2Value")
                .setLockRetry(1000, 50)
                .build();
    }

    private ITokenCacheAccessAspect createPersistenceAspect() throws IOException {
        return new PersistenceTokenCacheAccessAspect(createPersistenceSettings());
    }

    private ConfidentialClientApplication createConfidentialClient() throws IOException {

        IClientCredential clientCredential =
                ClientCredentialFactory.createFromSecret(TestData.CONFIDENTIAL_CLIENT_SECRET);

        return ConfidentialClientApplication.builder(TestData.CONFIDENTIAL_CLIENT_ID, clientCredential)
                .authority(TestData.TENANT_SPECIFIC_AUTHORITY)
                .setTokenCacheAccessAspect(createPersistenceAspect())
                .build();
    }

    // @Test
    public void silentlyGetPersistedTokensIntegrationTest() throws IOException, ExecutionException, InterruptedException {

        ConfidentialClientApplication app = createConfidentialClient();
        IAuthenticationResult interactiveResult = acquireTokenInteractively(app);

        Assert.assertNotNull(interactiveResult.accessToken());

        // create another application object, so it will not share in-memory cache with app
        ConfidentialClientApplication app1 = createConfidentialClient();
        IAuthenticationResult silentResult = acquireTokenSilently(app1);

        Assert.assertEquals(interactiveResult.accessToken(), silentResult.accessToken());
    }

    private IAuthenticationResult acquireTokenInteractively(ConfidentialClientApplication app){

        ClientCredentialParameters parameters =
                ClientCredentialParameters.builder(Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE)).build();

        CompletableFuture<IAuthenticationResult> future = app.acquireToken(parameters);

        return future.join();
    }

    private IAuthenticationResult acquireTokenSilently(ConfidentialClientApplication app) throws MalformedURLException {

        SilentParameters parameters =
                SilentParameters.builder(Collections.singleton(TestData.GRAPH_DEFAULT_SCOPE))
                        .build();

        CompletableFuture<IAuthenticationResult> future = app.acquireTokenSilently(parameters);

        return future.join();
    }
}