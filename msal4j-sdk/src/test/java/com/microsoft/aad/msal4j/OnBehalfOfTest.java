// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OnBehalfOfTest {

    @Test
    void OnBehalfOf_InternalCacheLookup_Success() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        TestHelper.mockSuccessfulTokenResponse(httpClientMock);

        ConfidentialClientApplication cca = TestHelper.buildCca(httpClientMock);

        OnBehalfOfParameters parameters = OnBehalfOfParameters.builder(TestHelper.TEST_SCOPE_SET, new UserAssertion(TestHelper.signedAssertion)).build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();
        IAuthenticationResult result2 = cca.acquireToken(parameters).get();

        //OBO flow should perform an internal cache lookup, so similar parameters should only cause one HTTP client call
        assertEquals(result.accessToken(), result2.accessToken());
        verify(httpClientMock, times(1)).send(any());
    }

    @Test
    void OnBehalfOf_TenantOverride() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca = TestHelper.buildCca(httpClientMock);

        HashMap<String, String> tokenResponseValues = new HashMap<>();
        tokenResponseValues.put("access_token", "accessTokenFirstCall");

        TestHelper.mockSuccessfulTokenResponse(httpClientMock, tokenResponseValues);
        OnBehalfOfParameters parameters = OnBehalfOfParameters.builder(TestHelper.TEST_SCOPE_SET, new UserAssertion(TestHelper.signedAssertion)).build();

        //The two acquireToken calls have the same parameters...
        IAuthenticationResult resultAppLevelTenant = cca.acquireToken(parameters).get();
        IAuthenticationResult resultAppLevelTenantCached = cca.acquireToken(parameters).get();
        //...so only one token should be added to the cache, and the mocked HTTP client's "send" method should only have been called once
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals(resultAppLevelTenant.accessToken(), resultAppLevelTenantCached.accessToken());
        verify(httpClientMock, times(1)).send(any());

        tokenResponseValues.put("access_token", "accessTokenSecondCall");

        TestHelper.mockSuccessfulTokenResponse(httpClientMock, tokenResponseValues);
        parameters = OnBehalfOfParameters.builder(TestHelper.TEST_SCOPE_SET, new UserAssertion(TestHelper.signedAssertion)).tenant("otherTenant").build();

        //Overriding the tenant parameter in the request should lead to a new token call being made...
        IAuthenticationResult resultRequestLevelTenant = cca.acquireToken(parameters).get();
        IAuthenticationResult resultRequestLevelTenantCached = cca.acquireToken(parameters).get();
        //...which should be different from the original token, and thus the cache should have two tokens created from two HTTP calls
        assertEquals(2, cca.tokenCache.accessTokens.size());
        assertEquals(resultRequestLevelTenant.accessToken(), resultRequestLevelTenantCached.accessToken());
        assertNotEquals(resultAppLevelTenant.accessToken(), resultRequestLevelTenant.accessToken());
        verify(httpClientMock, times(2)).send(any());
    }
}