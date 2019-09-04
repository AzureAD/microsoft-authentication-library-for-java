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
public class TokenRequestTest extends AbstractMsalTests {

    @Test
    public void executeOAuthRequest_SCBadRequestErrorInvalidGrant_InteractionRequiredException()
            throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException {

        TokenRequest request = createMockedTokenRequest();

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

        EasyMock.expect(request.toOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest);

        try {
            request.executeOauthRequestAndProcessResponse();
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

        TokenRequest request = createMockedTokenRequest();

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

        EasyMock.expect(request.toOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest);

        try {
            request.executeOauthRequestAndProcessResponse();
            Assert.fail("Expected MsalServiceException was not thrown");
        } catch (MsalServiceException ex) {
            Assert.assertEquals(claims.replace("\\", ""), ex.claims());
            Assert.assertTrue(!(ex instanceof MsalInteractionRequiredException));
        }
        PowerMock.verifyAll();
    }

    private TokenRequest createMockedTokenRequest() throws URISyntaxException, MalformedURLException {
        PublicClientApplication app = new PublicClientApplication.Builder("id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext("id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                null,
                null,
                null,
                new TelemetryManager(null, false));

        return PowerMock.createPartialMock(
                TokenRequest.class, new String[]{"toOauthHttpRequest"},
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                serviceBundle);
    }

    @Test
    public void testConstructor() throws MalformedURLException,
            URISyntaxException {

        PublicClientApplication app = new PublicClientApplication.Builder("id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(
                        "id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        final TokenRequest request = new TokenRequest(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null, null, null));
        Assert.assertNotNull(request);
    }

    @Test
    public void testToOAuthRequestNonEmptyCorrelationId()
            throws MalformedURLException, SerializeException, URISyntaxException {

        PublicClientApplication app = new PublicClientApplication.Builder("id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(
                        "id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        TokenRequest request = new TokenRequest(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null, null, null));
        Assert.assertNotNull(request);
        OAuthHttpRequest req = request.toOauthHttpRequest();
        Assert.assertNotNull(req);
        Assert.assertEquals(
                "corr-id",
                req.getReadOnlyExtraHeaderParameters().get(
                        ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
    }

    @Test
    public void testToOAuthRequestNullCorrelationId_NullClientAuth()
            throws MalformedURLException, SerializeException,
            URISyntaxException {

        PublicClientApplication app = new PublicClientApplication.Builder("id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(
                        "id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        final TokenRequest request = new TokenRequest(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null, null, null));
        Assert.assertNotNull(request);
        final OAuthHttpRequest req = request.toOauthHttpRequest();
        Assert.assertNotNull(req);
    }

    @Test
    public void testExecuteOAuth_Success() throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException {

        PublicClientApplication app = new PublicClientApplication.Builder("id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(
                        "id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                null,
                null,
                null,
                new TelemetryManager(null, false));

        final TokenRequest request = PowerMock.createPartialMock(
                TokenRequest.class, new String[] { "toOauthHttpRequest" },
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle);

        final OAuthHttpRequest msalOAuthHttpRequest = PowerMock
                .createMock(OAuthHttpRequest.class);

        final HTTPResponse httpResponse = PowerMock
                .createMock(HTTPResponse.class);

        EasyMock.expect(request.toOauthHttpRequest())
                .andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse)
                .times(1);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(200).times(1);
        EasyMock.expect(httpResponse.getContentAsJSONObject())
                .andReturn(
                        JSONObjectUtils
                                .parse(TestConfiguration.HTTP_RESPONSE_FROM_AUTH_CODE))
                .times(1);
        httpResponse.ensureStatusCode(200);
        EasyMock.expectLastCall();

        EasyMock.expect(httpResponse.getHeaderValue("User-Agent")).andReturn(null);
        EasyMock.expect(httpResponse.getHeaderValue("x-ms-request-id")).andReturn(null);
        EasyMock.expect(httpResponse.getHeaderValue("x-ms-clitelem")).andReturn(null);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(200).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest, httpResponse);

        final AuthenticationResult result = request.executeOauthRequestAndProcessResponse();
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

        PublicClientApplication app = new PublicClientApplication.Builder("id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr =  new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(
                        "id",
                        "corr-id",
                        PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                null,
                null,
                null,
                new TelemetryManager(null, false));

        final TokenRequest request = PowerMock.createPartialMock(
                TokenRequest.class, new String[] { "toOauthHttpRequest" },
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle);
        final OAuthHttpRequest msalOAuthHttpRequest = PowerMock
                .createMock(OAuthHttpRequest.class);

        final HTTPResponse httpResponse = PowerMock
                .createMock(HTTPResponse.class);
        EasyMock.expect(request.toOauthHttpRequest())
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
            request.executeOauthRequestAndProcessResponse();
            PowerMock.verifyAll();
        }
        finally {
            PowerMock.reset(request, msalOAuthHttpRequest, httpResponse,
                    TokenErrorResponse.class, errorResponse);
        }
    }
}