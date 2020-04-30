// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

import static org.easymock.EasyMock.anyObject;

public class RequestThrottlingTest extends AbstractMsalTests {

    public final Integer THROTTLE_IN_SEC = 1;

    @BeforeMethod
    void init(){
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = THROTTLE_IN_SEC;
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
            TokenEndpointResponseType responseType)
            throws Exception {
        IHttpClient httpClientMock = EasyMock.createMock(IHttpClient.class);

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

        if(responseType == TokenEndpointResponseType.STATUS_CODE_500){
            // expected to called two times due to retry logic
            EasyMock.expect(httpClientMock.send(anyObject())).andReturn(httpResponse).times(2);
        }
        else{
            EasyMock.expect(httpClientMock.send(anyObject())).andReturn(httpResponse).times(1);
        }

        PublicClientApplication app = getPublicClientApp(httpClientMock);

        PowerMock.replayAll(httpClientMock);
        return app;
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
                Assert.fail("Unexpected exception");
            }
        }
        PowerMock.verifyAll();
        PowerMock.resetAll();

        // repeat same request #1, should be throttled
        try {
            app = getPublicClientApp();
            app.acquireToken(getAcquireTokenApiParameters("scope1")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalThrottlingException)) {
                Assert.fail("Unexpected exception");
            }
        }

        // request #2 (different scope) should not be throttled
        app = getClientApplicationMockedWithOneTokenEndpointResponse(tokenEndpointResponseType);
        try {
            app.acquireToken(getAcquireTokenApiParameters("scope2")).join();
        } catch (Exception ex) {
            if (!(ex.getCause() instanceof MsalServiceException)) {
                Assert.fail("Unexpected exception");
            }
        }
        PowerMock.verifyAll();
        PowerMock.resetAll();

        // repeat request #1, should not be throttled after
        // throttling for this request got expired
        Thread.sleep(THROTTLE_IN_SEC * 1000);
        app = getClientApplicationMockedWithOneTokenEndpointResponse(tokenEndpointResponseType);
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

    @Test
    public void STSResponseContains_RetryAfterHeader() throws Exception {
        throttlingTest(TokenEndpointResponseType.RETRY_AFTER_HEADER);
    }

    @Test
    public void STSResponseContains_StatusCode429() throws Exception {
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_429);
    }

    @Test
    public void STSResponseContains_StatusCode429_RetryAfterHeader() throws Exception {
        // using big value for DEFAULT_THROTTLING_TIME_SEC to make sure that RetryAfterHeader value used instead
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = 1000;
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_429_RETRY_AFTER_HEADER);
    }

    @Test
    public void STSResponseContains_StatusCode500() throws Exception  {
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_500);
    }

    @Test
    public void STSResponseContains_StatusCode500_RetryAfterHeader() throws Exception  {
        // using big value for DEFAULT_THROTTLING_TIME_SEC to make sure that RetryAfterHeader value used instead
        ThrottlingCache.DEFAULT_THROTTLING_TIME_SEC = 1000;
        throttlingTest(TokenEndpointResponseType.STATUS_CODE_500_RETRY_AFTER_HEADER);
    }
}
