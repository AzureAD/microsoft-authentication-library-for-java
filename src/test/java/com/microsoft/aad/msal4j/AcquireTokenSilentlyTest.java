// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.concurrent.CompletionException;

@Test(groups = { "checkin" })
public class AcquireTokenSilentlyTest extends PowerMockTestCase {

    @Test(expectedExceptions = MsalClientException.class,
            expectedExceptionsMessageRegExp = AuthenticationErrorMessage.NO_TOKEN_IN_CACHE)
    public void publicAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        PublicClientApplication application = PublicClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID)
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();

        try {
            application.acquireTokenSilently(parameters).join();
        }
        catch (CompletionException ex){
            throw ex.getCause();
        }
    }

    @Test(expectedExceptions = MsalClientException.class,
            expectedExceptionsMessageRegExp = AuthenticationErrorMessage.NO_TOKEN_IN_CACHE)
    public void confidentialAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        ConfidentialClientApplication application = ConfidentialClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID, ClientCredentialFactory.create(TestConfiguration.AAD_CLIENT_SECRET))
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();

        try {
            application.acquireTokenSilently(parameters).join();
        }
        catch (CompletionException ex){
            throw ex.getCause();
        }
    }
}
