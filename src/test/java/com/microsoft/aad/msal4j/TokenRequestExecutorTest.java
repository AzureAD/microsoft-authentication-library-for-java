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
    public void executeOAuthRequest_SCBadRequestErrorInvalidGrant_InteractionRequiredException()
            throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException {

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
            IOException, URISyntaxException {

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
        PublicClientApplication app = PublicClientApplication.builder("id")
                .correlationId("corr_id").build();

        RefreshTokenParameters refreshTokenParameters = RefreshTokenParameters.
                builder(Collections.singleton("default-scope"), "rt").build();

        RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(
                refreshTokenParameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, refreshTokenParameters));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                new DefaultHttpClient(null, null),
                new TelemetryManager(null, false));

        return PowerMock.createPartialMock(
                TokenRequestExecutor.class, new String[]{"createOauthHttpRequest"},
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                refreshTokenRequest,
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
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

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
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

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
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

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
            IOException, URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

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
                                .parse(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE))
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
            ParseException, MsalException, IOException, URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

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
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(402).times(3);
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