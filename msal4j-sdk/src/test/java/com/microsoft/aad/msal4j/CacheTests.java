// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CacheTests {

    @Test
    void cacheLookup_MixAccountBasedAndAssertionBasedSilentFlows() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        HashMap<String, String> responseParameters = new HashMap<>();
        //Acquire a token with no ID token/account associated with it
        responseParameters.put("access_token", "accessTokenNoAccount");

        ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        IAuthenticationResult resultNoAccount = cca.acquireToken(clientCredentialParameters).get();

        //Ensure there is one token in the cache, and the result had no account
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertNull(resultNoAccount.account());
        verify(httpClientMock, times(1)).send(any());

        //Acquire a second token, this time with an ID token/account
        responseParameters.put("access_token", "accessTokenWithAccount");
        responseParameters.put("id_token", TestHelper.createIdToken(new HashMap<>()));

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        OnBehalfOfParameters onBehalfOfParametersarameters = OnBehalfOfParameters.builder(Collections.singleton("someOtherScopes"), new UserAssertion(TestHelper.signedAssertion)).build();
        IAuthenticationResult resultWithAccount = cca.acquireToken(onBehalfOfParametersarameters).get();

        //Ensure there are now two tokens in the cache, and the result has an account
        assertEquals(2, cca.tokenCache.accessTokens.size());
        assertNull(resultNoAccount.account());
        verify(httpClientMock, times(2)).send(any());

        //Make two silent calls, one with the account and one without
        SilentParameters silentParametersNoAccount = SilentParameters.builder(Collections.singleton("someScopes")).build();
        SilentParameters silentParametersWithAccount = SilentParameters.builder(Collections.singleton("someOtherScopes"), resultWithAccount.account()).build();

        resultNoAccount = cca.acquireTokenSilently(silentParametersNoAccount).get();
        resultWithAccount = cca.acquireTokenSilently(silentParametersWithAccount).get();

        //Ensure the correct access tokens were returned from each silent call
        assertEquals("accessTokenNoAccount", resultNoAccount.accessToken());
        assertEquals("accessTokenWithAccount", resultWithAccount.accessToken());
    }
}
