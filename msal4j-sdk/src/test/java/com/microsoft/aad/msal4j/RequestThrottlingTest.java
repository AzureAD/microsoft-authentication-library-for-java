// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    private boolean skipInvocationCountCheck = false;


    @BeforeEach
    void init() {
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = THROTTLE_IN_SEC;
    }

    @AfterEach
    void check() throws Exception {
        if (skipInvocationCountCheck) {
            return;
        }

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
                httpResponse.statusCode(HttpStatus.HTTP_OK);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE_ID_AND_ACCESS);

                headers.put("Retry-After", Arrays.asList(THROTTLE_IN_SEC.toString()));
                break;
            case STATUS_CODE_429:
                httpResponse.statusCode(HttpStatus.HTTP_TOO_MANY_REQUESTS);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                break;
            case STATUS_CODE_429_RETRY_AFTER_HEADER:
                httpResponse.statusCode(HttpStatus.HTTP_TOO_MANY_REQUESTS);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                headers.put("Retry-After", Arrays.asList(THROTTLE_IN_SEC.toString()));
                break;
            case STATUS_CODE_500:
                httpResponse.statusCode(HttpStatus.HTTP_INTERNAL_ERROR);
                httpResponse.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
                break;
            case STATUS_CODE_500_RETRY_AFTER_HEADER:
                httpResponse.statusCode(HttpStatus.HTTP_INTERNAL_ERROR);
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

    private UserNamePasswordParameters getUserNamePasswordApiParameters(String username, String scope) {
        return UserNamePasswordParameters
                .builder(Collections.singleton(scope), username, "password".toCharArray())
                .build();
    }

    // Regression test for issue #1019: a failed request for one user must not throttle a different
    // user under the same clientId/authority/scope.
    @Test
    void STSResponseContains_StatusCode500_DifferentUsersNotThrottledForEachOther() throws Exception {
        skipInvocationCountCheck = true;
        ThrottlingCache.clear();

        // user A's request fails with a 500 -> gets cached as a throttled request
        PublicClientApplication app =
                getClientApplicationMockedWithOneTokenEndpointResponse(TokenEndpointResponseType.STATUS_CODE_500);
        try {
            app.acquireToken(getUserNamePasswordApiParameters("userA@contoso.com", "scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("Unexpected exception");
            }
        }

        // repeating user A's request should be throttled
        try {
            app = getPublicClientApp();
            app.acquireToken(getUserNamePasswordApiParameters("userA@contoso.com", "scope1")).join();
            fail("Expected MsalThrottlingException");
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalThrottlingException)) {
                fail("Unexpected exception");
            }
        }

        // user B, same clientId/authority/scope, must NOT be throttled by user A's failure
        app = getClientApplicationMockedWithOneTokenEndpointResponse(TokenEndpointResponseType.STATUS_CODE_500);
        try {
            app.acquireToken(getUserNamePasswordApiParameters("userB@contoso.com", "scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                fail("User B should not be throttled by user A's failed request");
            }
        }
    }

    // Confirms that per-account throttling isolation for the silent flow (previously driven by the
    // `instanceof SilentParameters` special case) is preserved now that the fingerprint is derived
    // from RequestContext.userIdentifier() instead.
    @Test
    void SilentFlow_DifferentAccountsThrottledIndependently() throws Exception {
        skipInvocationCountCheck = true;
        ThrottlingCache.clear();

        PublicClientApplication app = getPublicClientApp();

        IHttpClient localHttpClientMock = mock(IHttpClient.class);
        HttpResponse http500 = new HttpResponse();
        http500.statusCode(HttpStatus.HTTP_INTERNAL_ERROR);
        http500.body(TestConfiguration.TOKEN_ENDPOINT_INVALID_GRANT_ERROR_RESPONSE);
        http500.addHeaders(Collections.singletonMap("Content-Type", Collections.singletonList("application/json")));
        doReturn(http500).when(localHttpClientMock).send(any());

        HttpHelper httpHelper = new HttpHelper(localHttpClientMock, new DefaultRetryPolicy());
        ServiceBundle serviceBundle = new ServiceBundle(null, new TelemetryManager(null, false), httpHelper);

        HttpRequest httpRequest = new HttpRequest(HttpMethod.POST,
                "https://login.microsoftonline.com/common/oauth2/v2.0/token");

        IAccount accountA = mock(IAccount.class);
        doReturn("oidA.tidA").when(accountA).homeAccountId();
        SilentParameters paramsA = SilentParameters.builder(Collections.singleton("scope1"), accountA).build();
        RequestContext contextA = new RequestContext(app, PublicApi.ACQUIRE_TOKEN_SILENTLY, paramsA,
                UserIdentifier.fromHomeAccountId(accountA.homeAccountId()));

        // account A's request fails with a 500 -> gets cached as a throttled request
        httpHelper.executeHttpRequest(httpRequest, contextA, serviceBundle);
        // repeating account A's request should be throttled
        assertThrows(MsalThrottlingException.class,
                () -> httpHelper.executeHttpRequest(httpRequest, contextA, serviceBundle));

        // account B, same clientId/authority/scope, must NOT be throttled by account A's failure
        IAccount accountB = mock(IAccount.class);
        doReturn("oidB.tidB").when(accountB).homeAccountId();
        SilentParameters paramsB = SilentParameters.builder(Collections.singleton("scope1"), accountB).build();
        RequestContext contextB = new RequestContext(app, PublicApi.ACQUIRE_TOKEN_SILENTLY, paramsB,
                UserIdentifier.fromHomeAccountId(accountB.homeAccountId()));

        assertDoesNotThrow(() -> httpHelper.executeHttpRequest(httpRequest, contextB, serviceBundle));
    }
}
