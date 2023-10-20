// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcquireTokenSilentlyTest {

    @Test
    void publicAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        PublicClientApplication application = PublicClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID)
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();

        CompletableFuture<IAuthenticationResult> future = application.acquireTokenSilently(parameters);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);

        assertTrue(ex.getCause() instanceof MsalClientException);
        assertTrue(ex.getMessage().contains(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE));
    }

    @Test
    void confidentialAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        ConfidentialClientApplication application = ConfidentialClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID, ClientCredentialFactory.createFromSecret(TestConfiguration.AAD_CLIENT_DUMMYSECRET))
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();
        CompletableFuture<IAuthenticationResult> future = application.acquireTokenSilently(parameters);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);

        assertTrue(ex.getCause() instanceof MsalClientException);
        assertTrue(ex.getMessage().contains(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE));
    }
}
