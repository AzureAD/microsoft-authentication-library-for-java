// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

import static org.easymock.EasyMock.anyObject;

public class UIRequiredCacheTest extends AbstractMsalTests  {

    public final Integer CACHING_TIME_SEC = 2;

    @BeforeClass
    void init(){
        UiRequiredCache.DEFAULT_CACHING_TIME_SEC = CACHING_TIME_SEC;
    }

    private RefreshTokenParameters getAcquireTokenApiParameters(String scope) {
        return RefreshTokenParameters
                .builder(Collections.singleton(scope), "refreshToken")
                .build();
    }

    private RefreshTokenParameters getAcquireTokenApiParameters() {
        return getAcquireTokenApiParameters("default-scope");
    }

    private PublicClientApplication getPublicClientApp() throws Exception {
        return getPublicClientApp(null);
    }

    private PublicClientApplication getPublicClientApp(IHttpClient httpClient) throws Exception {
        String instanceDiscoveryResponse = TestHelper.readResource(
                this.getClass(),
                "/instance_discovery_data/aad_instance_discovery_response_valid.json");

        PublicClientApplication.Builder appBuilder = PublicClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID + "1")
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse);

        if (httpClient != null) {
            appBuilder.httpClient(httpClient);
        }

        return appBuilder.build();
    }

    private PublicClientApplication getClientApplicationMockedWithOneTokenEndpointResponse()
            throws Exception {
        IHttpClient httpClientMock = EasyMock.createMock(IHttpClient.class);

        HttpResponse httpResponse = new HttpResponse();
        Map<String, List<String>> headers = new HashMap<>();

        httpResponse.statusCode(400);
        httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);

        headers.put("Content-Type", Arrays.asList("application/json"));
        httpResponse.addHeaders(headers);

        EasyMock.expect(httpClientMock.send(anyObject())).andReturn(httpResponse).times(1);
        PowerMock.replayAll(httpClientMock);

        return getPublicClientApp(httpClientMock);
    }

    @Test
    public void SilentRequest_STSResponseInvalidGrantError_repeatedRequestsServedFromCache() throws Exception {
        // refresh token request #1 to token endpoint
        // response contains invalid grant error
        PublicClientApplication
                app = getClientApplicationMockedWithOneTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                Assert.fail("Unexpected exception");
            }
        }
        PowerMock.verifyAll();
        PowerMock.resetAll();

        // repeat same request #1, cached response should be returned
        try {
            app = getPublicClientApp();
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                Assert.fail("Unexpected exception");
            }
        }

        // request #2 (different scope) should not be served from cache
        // request to token endpoint should be sent
        app = getClientApplicationMockedWithOneTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope2")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                Assert.fail("Unexpected exception");
            }
        }
        PowerMock.verifyAll();
        PowerMock.resetAll();

        // repeat request #1, should not be served from cache (cache entry should be expired)
        // request to token endpoint should be sent
        Thread.sleep(CACHING_TIME_SEC * 1000);
        app = getClientApplicationMockedWithOneTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                Assert.fail("Unexpected exception");
            }
        }
        PowerMock.verifyAll();
        PowerMock.resetAll();
    }
}
