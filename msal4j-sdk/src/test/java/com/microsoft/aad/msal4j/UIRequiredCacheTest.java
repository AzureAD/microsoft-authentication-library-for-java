// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.BeforeAll;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UIRequiredCacheTest {

    final Integer CACHING_TIME_SEC = 2;

    @BeforeAll
    void init() {
        InteractionRequiredCache.DEFAULT_CACHING_TIME_SEC = CACHING_TIME_SEC;
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

    private HttpResponse getHttpResponse(int statusCode, String body) {
        HttpResponse httpResponse = new HttpResponse();
        Map<String, List<String>> headers = new HashMap<>();

        httpResponse.statusCode(statusCode);
        httpResponse.body(body);

        headers.put("Content-Type", Arrays.asList("application/json"));
        httpResponse.addHeaders(headers);

        return httpResponse;
    }

    private PublicClientApplication getApp_MockedWith_InvalidGrantTokenEndpointResponse()
            throws Exception {
        IHttpClient httpClientMock = mock(IHttpClient.class);

        HttpResponse httpResponse = getHttpResponse(400,
                TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);

        doReturn(httpResponse).when(httpClientMock).send(any());

        return getPublicClientApp(httpClientMock);
    }

    private PublicClientApplication getApp_MockedWith_OKTokenEndpointResponse_InvalidGrantTokenEndpointResponse()
            throws Exception {
        IHttpClient httpClientMock = mock(IHttpClient.class);

        HttpResponse httpResponse =
                getHttpResponse(HTTPResponse.SC_OK, TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE);
        lenient().doReturn(httpResponse).when(httpClientMock).send(any());

        httpResponse = getHttpResponse(HTTPResponse.SC_UNAUTHORIZED,
                TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
        lenient().doReturn(httpResponse).when(httpClientMock).send(any());

        PublicClientApplication app = getPublicClientApp(httpClientMock);

        return app;
    }

    @Test
    void RefreshTokenRequest_STSResponseInvalidGrantError_repeatedRequestsServedFromCache() throws Exception {
        InteractionRequiredCache.clear();

        // refresh token request #1 to token endpoint
        // response contains invalid grant error
        PublicClientApplication
                app = getApp_MockedWith_InvalidGrantTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                fail("Unexpected exception");
            }
        }

        // repeat same request #1, cached response should be returned
        try {
            app = getPublicClientApp();
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                fail("Unexpected exception");
            }
        }

        // request #2 (different scope) should not be served from cache
        // request to token endpoint should be sent
        app = getApp_MockedWith_InvalidGrantTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope2")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                fail("Unexpected exception");
            }
        }

        // repeat request #1, should not be served from cache (cache entry should be expired)
        // request to token endpoint should be sent
        Thread.sleep(CACHING_TIME_SEC * 1000);
        app = getApp_MockedWith_InvalidGrantTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("Unexpected exception");
            }
        }
    }

    @Test
    void SilentRequest_STSResponseInvalidGrantError_repeatedRequestsServedFromCache() throws Exception {
        InteractionRequiredCache.clear();

        PublicClientApplication
                app = getApp_MockedWith_OKTokenEndpointResponse_InvalidGrantTokenEndpointResponse();

        // silent request #1 fails with InteractionRequiredException, response cached
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
            SilentParameters silentParameters = SilentParameters.builder(
                    Collections.singleton("scope1"),
                    app.getAccounts().join().iterator().next())
                    .build();
            app.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                fail("Unexpected exception");
            }
        }

        // silent request #2 (different scope) should not be served from cache
        // request to token endpoint should be sent
        app = getApp_MockedWith_OKTokenEndpointResponse_InvalidGrantTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
            SilentParameters silentParameters = SilentParameters.builder(
                    Collections.singleton("scope2"),
                    app.getAccounts().join().iterator().next())
                    .build();
            app.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalInteractionRequiredException)) {
                fail("Unexpected exception");
            }
        }

        // repeat request #1, should not be served from cache (cache entry should be expired)
        // request to token endpoint should be sent
        Thread.sleep(CACHING_TIME_SEC * 1000);
        app = getApp_MockedWith_OKTokenEndpointResponse_InvalidGrantTokenEndpointResponse();
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
            SilentParameters silentParameters = SilentParameters.builder(
                    Collections.singleton("scope1"),
                    app.getAccounts().join().iterator().next())
                    .build();
            app.acquireTokenSilently(silentParameters).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("Unexpected exception");
            }
        }
    }
}
