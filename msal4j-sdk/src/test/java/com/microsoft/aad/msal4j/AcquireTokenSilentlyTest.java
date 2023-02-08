// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Assertions;
import org.powermock.modules.testng.PowerMockTestCase;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.assertEquals;

//@Test//(groups = {"checkin"})
class AcquireTokenSilentlyTest  {

    @Test
    void publicAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        PublicClientApplication application = PublicClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID)
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        MsalClientException exception = Assertions.assertThrows(MsalClientException.class, () -> {
            SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();
            try {
                application.acquireTokenSilently(parameters).join();
            } catch (CompletionException ex) {
                throw ex.getCause();
            }
        });

        assertEquals(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, exception.getMessage());
    }

    @Test
    void confidentialAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        ConfidentialClientApplication application = ConfidentialClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID, ClientCredentialFactory.createFromSecret(TestConfiguration.AAD_CLIENT_DUMMYSECRET))
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        MsalClientException exception = Assertions.assertThrows(MsalClientException.class, () -> {
            SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();

            try {
                application.acquireTokenSilently(parameters).join();
            } catch (CompletionException ex) {
                throw ex.getCause();
            }
        });

        assertEquals(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE, exception.getMessage());
    }
}
