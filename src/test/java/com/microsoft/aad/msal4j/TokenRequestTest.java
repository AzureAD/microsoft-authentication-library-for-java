// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import net.minidev.json.JSONObject;
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

@Test(groups = { "checkin" })
@PrepareForTest(TokenErrorResponse.class)
public class TokenRequestTest extends AbstractMsalTests {

    @Test
    public void executeOAuthRequest_SCBadRequestErrorInteractionRequired_ClaimsChallengeExceptionThrown()
            throws SerializeException, ParseException, AuthenticationException,
            IOException, URISyntaxException {

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
                new TelemetryManager(null, false));

        TokenRequest request = PowerMock.createPartialMock(
                TokenRequest.class, new String[]{"toOauthHttpRequest"},
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle);

        OAuthHttpRequest msalOAuthHttpRequest = PowerMock
                .createMock(OAuthHttpRequest.class);

        HTTPResponse httpResponse = new HTTPResponse(HTTPResponse.SC_BAD_REQUEST);

        String claims = "{\\\"access_token\\\":{\\\"polids\\\":{\\\"essential\\\":true,\\\"values\\\":[\\\"5ce770ea-8690-4747-aa73-c5b3cd509cd4\\\"]}}}";

        String content = "{\"error\":\"interaction_required\"," +
                "\"error_description\":\"AADSTS50076: description\\r\\nCorrelation ID: 3a...5a\\r\\nTimestamp:2017-07-15 02:35:05Z\"," +
                "\"error_codes\":[50076]," +
                "\"timestamp\":\"2017-07-15 02:35:05Z\"," +
                "\"trace_id\":\"0788...000\"," +
                "\"correlation_id\":\"3a...95a\"," +
                "\"claims\":\"" + claims + "\"}";
        httpResponse.setContent(content);
        httpResponse.setContentType(CommonContentTypes.APPLICATION_JSON);

        EasyMock.expect(request.toOauthHttpRequest()).andReturn(msalOAuthHttpRequest).times(1);
        EasyMock.expect(msalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, msalOAuthHttpRequest);

        try {
            request.executeOauthRequestAndProcessResponse();
            Assert.fail("Expected ClaimsChallengeException was not thrown");
        } catch (ClaimsChallengeException ex) {
            Assert.assertEquals(claims.replace("\\", ""), ex.claims());
        }
        PowerMock.verifyAll();
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
                new ServiceBundle(null, null, null, null));
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
                new ServiceBundle(null, null, null, null));
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
                new ServiceBundle(null, null, null, null));
        Assert.assertNotNull(request);
        final OAuthHttpRequest req = request.toOauthHttpRequest();
        Assert.assertNotNull(req);
    }

    @Test
    public void testExecuteOAuth_Success() throws SerializeException, ParseException, AuthenticationException,
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

    @Test(expectedExceptions = AuthenticationException.class)
    public void testExecuteOAuth_Failure() throws SerializeException,
            ParseException, AuthenticationException, IOException, URISyntaxException {

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
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(402).times(1);

        final TokenErrorResponse errorResponse = PowerMock
                .createMock(TokenErrorResponse.class);

        final ErrorObject errorObject = PowerMock.createMock(ErrorObject.class);

        EasyMock.expect(errorObject.getCode())
                .andReturn("unknown").times(3);
        EasyMock.expect(errorObject.getHTTPStatusCode())
                .andReturn(402).times(1);

        EasyMock.expect(errorResponse.getErrorObject())
                .andReturn(errorObject).times(1);

        EasyMock.expect(httpResponse.getHeaderValue("User-Agent")).andReturn(null);
        EasyMock.expect(httpResponse.getHeaderValue("x-ms-request-id")).andReturn(null);
        EasyMock.expect(httpResponse.getHeaderValue("x-ms-clitelem")).andReturn(null);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(402).times(1);


        PowerMock.mockStaticPartial(TokenErrorResponse.class, "parse");
        PowerMock.createPartialMock(TokenErrorResponse.class, "parse");
        EasyMock.expect(TokenErrorResponse.parse(httpResponse))
                .andReturn(errorResponse).times(1);

        final JSONObject jsonObj = PowerMock.createMock(JSONObject.class);
        EasyMock.expect(jsonObj.toJSONString())
                .andReturn(TestConfiguration.HTTP_ERROR_RESPONSE).times(1);
        EasyMock.expect(errorResponse.toJSONObject()).andReturn(jsonObj)
                .times(1);

        PowerMock.replay(request, msalOAuthHttpRequest, httpResponse,
                TokenErrorResponse.class, errorObject, jsonObj, errorResponse);
        try {
            request.executeOauthRequestAndProcessResponse();
            PowerMock.verifyAll();
        }
        finally {
            PowerMock.reset(request, msalOAuthHttpRequest, httpResponse,
                    TokenErrorResponse.class, jsonObj, errorResponse);
        }
    }
}