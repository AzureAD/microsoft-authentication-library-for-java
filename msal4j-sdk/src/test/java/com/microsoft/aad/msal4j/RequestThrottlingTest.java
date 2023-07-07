// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
class RequestThrottlingTest {

    public final Integer THROTTLE_IN_SEC = 1;
    public TokenEndpointResponseType responseType;
    IHttpClient httpClientMock = mock(IHttpClient.class);


    @BeforeEach
    void init() {
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = THROTTLE_IN_SEC;
    }

    @AfterEach
    void check() throws Exception {

        //throttlingTest() makes three non-throttled calls, so for a test without a retry there should be
        // 3 invocations of httpClientMock.send(), and 6 invocations if the calls are set to retry
        if (responseType == TokenEndpointResponseType.STATUS_CODE_500) {
            verify(httpClientMock, times(6)).send(any());
        } else {
            verify(httpClientMock, times(3)).send(any());
        }
    }

    private AuthorizationCodeParameters getAcquireTokenApiParameters(String scope) throws URISyntaxException {
        return AuthorizationCodeParameters
                .builder("auth_code",
                        new URI(TestConfiguration.AAD_DEFAULT_REDIRECT_URI))
                .scopes(Collections.singleton(scope))
                .build();
    }

    private AuthorizationCodeParameters getAcquireTokenApiParameters() throws URISyntaxException {
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
                .builder(TestConfiguration.AAD_CLIENT_ID)
                .aadInstanceDiscoveryResponse(instanceDiscoveryResponse);

        if (httpClient != null) {
            appBuilder.httpClient(httpClient);
        }

        return appBuilder.build();
    }

    private enum TokenEndpointResponseType {
        RETRY_AFTER_HEADER,
        STATUS_CODE_429,
        STATUS_CODE_429_RETRY_AFTER_HEADER,
        STATUS_CODE_500,
        STATUS_CODE_500_RETRY_AFTER_HEADER
    }

    private PublicClientApplication getClientApplicationMockedWithOneTokenEndpointResponse(
            TokenEndpointResponseType type)
            throws Exception {
        responseType = type;

        HttpResponse httpResponse = new HttpResponse();
        Map<String, List<String>> headers = new HashMap<>();

        switch (responseType) {
            case RETRY_AFTER_HEADER:
                httpResponse.statusCode(HTTPResponse.SC_OK);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE);

                headers.put("Retry-After", Arrays.asList(THROTTLE_IN_SEC.toString()));
                break;
            case STATUS_CODE_429:
                httpResponse.statusCode(429);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                break;
            case STATUS_CODE_429_RETRY_AFTER_HEADER:
                httpResponse.statusCode(429);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                headers.put("Retry-After", Arrays.asList(THROTTLE_IN_SEC.toString()));
                break;
            case STATUS_CODE_500:
                httpResponse.statusCode(500);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                break;
            case STATUS_CODE_500_RETRY_AFTER_HEADER:
                httpResponse.statusCode(500);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                headers.put("Retry-After", Arrays.asList(THROTTLE_IN_SEC.toString()));
                break;
        }
        headers.put("Content-Type", Arrays.asList("application/json"));
        httpResponse.addHeaders(headers);

        doReturn(httpResponse).when(httpClientMock).send(any());

        return getPublicClientApp(httpClientMock);
    }

    private void throttlingTest(TokenEndpointResponseType tokenEndpointResponseType) throws Exception {
        ThrottlingCache.clear();
        // request #1 to token endpoint
        // response contains Retry-After header
        PublicClientApplication
                app = getClientApplicationMockedWithOneTokenEndpointResponse(tokenEndpointResponseType);
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("Unexpected exception");
            }
        }

        // repeat same request #1, should be throttled
        try {
            app = getPublicClientApp();
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalThrottlingException)) {
                fail("Unexpected exception");
            }
        }

        // request #2 (different scope) should not be throttled
        app = getClientApplicationMockedWithOneTokenEndpointResponse(tokenEndpointResponseType);
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope2")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("Unexpected exception");
            }
        }

        // repeat request #1, should not be throttled after
        // throttling for this request got expired
        Thread.sleep(THROTTLE_IN_SEC * 1000);
        app = getClientApplicationMockedWithOneTokenEndpointResponse(tokenEndpointResponseType);
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("Unexpected exception");
            }
        }
    }

    @Test
    void STSResponseContains_RetryAfterHeader() throws Exception {
        throttlingTest(TokenEndpointResponseType.RETRY_AFTER_HEADER);
    }

    @Test
    void STSResponseContains_StatusCode429() throws Exception {
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_429);
    }

    @Test
    void STSResponseContains_StatusCode429_RetryAfterHeader() throws Exception {
        // using big value for DEFAULT_THROTTLING_TIME_SEC to make sure that RetryAfterHeader value used instead
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = 1000;
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_429_RETRY_AFTER_HEADER);
    }

    @Test
    void STSResponseContains_StatusCode500() throws Exception {
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_500);
    }

    @Test
    void STSResponseContains_StatusCode500_RetryAfterHeader() throws Exception {
        // using big value for DEFAULT_THROTTLING_TIME_SEC to make sure that RetryAfterHeader value used instead
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = 1000;
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_500_RETRY_AFTER_HEADER);
    }
}
