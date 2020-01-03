// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;

@Test(groups = { "checkin" })
@PrepareForTest(TokenErrorResponse.class)
public class TokenRequestExecutorTest extends AbstractMsalTests {

    @Test
    public void executeOauthRequest_NoAccessTokenOrRefreshTokenInResponse()
            throws URISyntaxException, IOException, ParseException, java.text.ParseException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock.createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_OK);

        // Response returned from B2C when you chose not to request AT or RT in azure portal.
        String content = "{\"id_token\":\"eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6Ilg1ZVhrNHh5b2pORnVtMWtsMll0djhk" +
                "bE5QNC1jNTdkTzZRR1RWQndhTmsifQ.eyJleHAiOjE1NzgwODgxOTksIm5iZiI6MTU3ODA4NDU5OSwidmVyIjoiMS4wIiwiaXNzIjo" +
                "iaHR0cHM6Ly9zYWdvbnphbGIyYy5iMmNsb2dpbi5jb20vZGRjYmVhMGMtZDM5Yi00MzY2LTg0MzUtODEwMjg0Nzk1MWY5L3YyLjAvI" +
                "iwic3ViIjoiMGIzOTBkNWQtOTE2ZS00MmY5LWI2ZjItZWUwNWEzNzlkMDA4IiwiYXVkIjoiZTU0NmE4ZTktZDZmMi00OTNmLWE3ZDc" +
                "tNDFjMDEzMDc0ZWVlIiwibm9uY2UiOiIzZWE5Y2MzNi01MzM0LTQwMjctYjI0OC0wNjM4ZWVkYWEwYWMiLCJpYXQiOjE1NzgwODQ1" +
                "OTksImF1dGhfdGltZSI6MTU3ODA4NDU5OSwiZ2l2ZW5fbmFtZSI6IlNhbnRpYWdvIiwidGZwIjoiQjJDXzFfZWRpdF9wcm9maWxlIn0" +
                ".j_Cym4POWxgrMQ1CcOV4gwSASpbXzVhAMw6eL74ZGy-eevb_Wn-n7sJ0Mj0gte6gEatONRUee6xlRxLaslwopm89XgQ30z5IEauG7aTg" +
                "w_Qb4rvCT8kEIlCSUl37R72R-vJpw2t-TM2rrbjz7hYSV9QopBUYxm3BDEnVkmCvLgs0tmcbPvF3i46iLpB4P4yWC9Gdn_udJ67_dTUgQQ_" +
                "Sz2kGCmB0urdv7bCaazw7YFtOifWV-DQTym_lfVQadBCpqpDRdRsM42-HFarCOgQenq9AVQ4WmPLXG_RudbtIp07Flqlxg-Cq2wdDfNI" +
                "tkSZpLZLQGpKWTfGWPFUPjb6H9Q\",\"token_type\":\"Bearer\",\"not_before\":1578084599,\"client_info\":\"eyJ1aW" +
                "QiOiIwYjM5MGQ1ZC05MTZlLTQyZjktYjZmMi1lZTA1YTM3OWQwMDgtYjJjXzFfZWRpdF9wcm9maWxlIiwidXRpZCI6ImRkY2JlYTBjLWQzOW" +
                "ItNDM2Ni04NDM1LTgxMDI4NDc5NTFmOSJ9\",\"scope\":\"\"}";

        httpResponse.setContent(content);
        httpResponse.setContentType(CommonContentTypes.APPLICATION_JSON);

        EasyMock.expect(request.createOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);
        PowerMock.replay(request, msalOAuthHttpRequest);

        AuthenticationResult result = request.executeTokenRequest();

        Assert.assertNotNull(result.idToken());
        Assert.assertNotNull(result.account());
        Assert.assertNull(result.accessToken());
        Assert.assertNull(result.refreshToken());
        // Assert.assertEquals(); //TODO: what should be the expiry date in AuthenticationResult when response does not contain expires_on?
    }

    @Test
    public void executeOAuthRequest_SCBadRequestErrorInvalidGrant_InteractionRequiredException()
            throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException, java.text.ParseException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock.createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_BAD_REQUEST);

        String claims = "{\\\"access_token\\\":{\\\"polids\\\":{\\\"essential\\\":true,\\\"values\\\":[\\\"5ce770ea-8690-4747-aa73-c5b3cd509cd4\\\"]}}}";

        String content = "{\"error\":\"invalid_grant\"," +
                "\"error_description\":\"AADSTS65001: description\\r\\nCorrelation ID: 3a...5a\\r\\nTimestamp:2017-07-15 02:35:05Z\"," +
                "\"error_codes\":[50076]," +
                "\"timestamp\":\"2017-07-15 02:35:05Z\"," +
                "\"trace_id\":\"0788...000\"," +
                "\"correlation_id\":\"3a...95a\"," +
                "\"suberror\":\"basic_action\"," +
                "\"claims\":\"" + claims + "\"}";
        httpResponse.setContent(content);
        httpResponse.setContentType(CommonContentTypes.APPLICATION_JSON);

        EasyMock.expect(request.createOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);
        PowerMock.replay(request, msalOAuthHttpRequest);

        try {
            request.executeTokenRequest();
            Assert.fail("Expected MsalServiceException was not thrown");
        } catch (MsalInteractionRequiredException ex) {
            Assert.assertEquals(claims.replace("\\", ""), ex.claims());
            Assert.assertEquals(ex.reason(), InteractionRequiredExceptionReason.BASIC_ACTION);
        }
        PowerMock.verifyAll();
    }

    @Test
    public void executeOAuthRequest_SCBadRequestErrorInvalidGrant_SubErrorFilteredServiceExceptionThrown()
            throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException, java.text.ParseException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock
                .createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_BAD_REQUEST);

        String claims = "{\\\"access_token\\\":{\\\"polids\\\":{\\\"essential\\\":true,\\\"values\\\":[\\\"5ce770ea-8690-4747-aa73-c5b3cd509cd4\\\"]}}}";

        String content = "{\"error\":\"invalid_grant\"," +
                "\"error_description\":\"AADSTS65001: description\\r\\nCorrelation ID: 3a...5a\\r\\nTimestamp:2017-07-15 02:35:05Z\"," +
                "\"error_codes\":[50076]," +
                "\"timestamp\":\"2017-07-15 02:35:05Z\"," +
                "\"trace_id\":\"0788...000\"," +
                "\"correlation_id\":\"3a...95a\"," +
                "\"suberror\":\"client_mismatch\"," +
                "\"claims\":\"" + claims + "\"}";
        httpResponse.setContent(content);
        httpResponse.setContentType(CommonContentTypes.APPLICATION_JSON);

        EasyMock.expect(request.createOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest);

        try {
            request.executeTokenRequest();
            Assert.fail("Expected MsalServiceException was not thrown");
        } catch (MsalServiceException ex) {
            Assert.assertEquals(claims.replace("\\", ""), ex.claims());
            Assert.assertTrue(!(ex instanceof MsalInteractionRequiredException));
        }
        PowerMock.verifyAll();
    }

    private TokenRequestExecutor createMockedTokenRequest() throws URISyntaxException, MalformedURLException {
        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr_id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                new DefaultHttpClient(null, null),
                new TelemetryManager(null, false));

        return PowerMock.createPartialMock(
                TokenRequestExecutor.class, new String[]{"createOauthHttpRequest"},
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                serviceBundle);
    }

    @Test
    public void testConstructor() throws MalformedURLException,
            URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        final TokenRequestExecutor request = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null));
        Assert.assertNotNull(request);
    }

    @Test
    public void testToOAuthRequestNonEmptyCorrelationId()
            throws MalformedURLException, SerializeException, URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        TokenRequestExecutor request = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null));
        Assert.assertNotNull(request);
        OAuthHttpRequest req = request.createOauthHttpRequest();
        Assert.assertNotNull(req);
        Assert.assertEquals(
                "corr-id",
                req.getExtraHeaderParams().get(HttpHeaders.CORRELATION_ID_HEADER_NAME));
    }

    @Test
    public void testToOAuthRequestNullCorrelationId_NullClientAuth()
            throws MalformedURLException, SerializeException,
            URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        final TokenRequestExecutor request = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null));
        Assert.assertNotNull(request);
        final OAuthHttpRequest req = request.createOauthHttpRequest();
        Assert.assertNotNull(req);
    }

    @Test
    public void testExecuteOAuth_Success() throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException, java.text.ParseException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                null,
                new TelemetryManager(null, false));

        final TokenRequestExecutor request = PowerMock.createPartialMock(
                TokenRequestExecutor.class, new String[] { "createOauthHttpRequest" },
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle);

        final OAuthHttpRequest msalOAuthHttpRequest = PowerMock
                .createMock(OAuthHttpRequest.class);

        final HTTPResponse httpResponse = PowerMock
                .createMock(HTTPResponse.class);

        EasyMock.expect(request.createOauthHttpRequest())
                .andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse)
                .times(1);
        EasyMock.expect(httpResponse.getContentAsJSONObject())
                .andReturn(
                        JSONObjectUtils
                                .parse(TestConfiguration.HTTP_RESPONSE_FROM_AUTH_CODE))
                .times(1);
        httpResponse.ensureStatusCode(200);
        EasyMock.expectLastCall();

        EasyMock.expect(httpResponse.getStatusCode()).andReturn(200).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest, httpResponse);

        final AuthenticationResult result = request.executeTokenRequest();
        PowerMock.verifyAll();

        Assert.assertNotNull(result.account());
        Assert.assertNotNull(result.account().homeAccountId());
        Assert.assertEquals(result.account().username(), "idlab@msidlab4.onmicrosoft.com");

        Assert.assertFalse(StringHelper.isBlank(result.accessToken()));
        Assert.assertFalse(StringHelper.isBlank(result.refreshToken()));
    }

    @Test(expectedExceptions = MsalException.class)
    public void testExecuteOAuth_Failure() throws SerializeException,
            ParseException, MsalException, IOException, URISyntaxException, java.text.ParseException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                null,
                new TelemetryManager(null, false));

        final TokenRequestExecutor request = PowerMock.createPartialMock(
                TokenRequestExecutor.class, new String[] { "createOauthHttpRequest" },
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle);
        final OAuthHttpRequest msalOAuthHttpRequest = PowerMock
                .createMock(OAuthHttpRequest.class);

        final HTTPResponse httpResponse = PowerMock
                .createMock(HTTPResponse.class);
        EasyMock.expect(request.createOauthHttpRequest())
                .andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse)
                .times(1);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(402).times(2);
        EasyMock.expect(httpResponse.getStatusMessage()).andReturn("403 Forbidden");
        EasyMock.expect(httpResponse.getHeaderMap()).andReturn(new HashMap<>());
        EasyMock.expect(httpResponse.getContent()).andReturn(TestConfiguration.HTTP_ERROR_RESPONSE);

        final ErrorResponse errorResponse = PowerMock.createMock(ErrorResponse.class);


        EasyMock.expect(errorResponse.error()).andReturn("invalid_request");

        EasyMock.expect(httpResponse.getHeaderValue("User-Agent")).andReturn(null);
        EasyMock.expect(httpResponse.getHeaderValue("x-ms-request-id")).andReturn(null);
        EasyMock.expect(httpResponse.getHeaderValue("x-ms-clitelem")).andReturn(null);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(402).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest, httpResponse,
                TokenErrorResponse.class, errorResponse);
        try {
            request.executeTokenRequest();
            PowerMock.verifyAll();
        }
        finally {
            PowerMock.reset(request, msalOAuthHttpRequest, httpResponse,
                    TokenErrorResponse.class, errorResponse);
        }
    }
}