// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.nimbusds.oauth2.sdk.*;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientSecretPost;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import net.minidev.json.JSONObject;
import org.easymock.EasyMock;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = { "checkin" })
@PrepareForTest(TokenErrorResponse.class)
public class AdalTokenRequestTest extends AbstractAdalTests {

    @Test
    public void executeOAuthRequest_SCBadRequestErrorInteractionRequired_AdalClaimsChallengeExceptionThrown()
            throws SerializeException,
            ParseException, AuthenticationException, IOException,
            java.text.ParseException, URISyntaxException {

        AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag, (String) null);

        AdalTokenRequest request = PowerMock.createPartialMock(
                AdalTokenRequest.class, new String[]{"toOAuthRequest"},
                new URL("http://login.windows.net"), null, grant, null, null, null);
        AdalOAuthRequest adalOAuthHttpRequest = PowerMock
                .createMock(AdalOAuthRequest.class);

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

        EasyMock.expect(request.toOAuthRequest()).andReturn(adalOAuthHttpRequest).times(1);
        EasyMock.expect(adalOAuthHttpRequest.send()).andReturn(httpResponse).times(1);

        PowerMock.replay(request, adalOAuthHttpRequest);

        try {
            request.executeOAuthRequestAndProcessResponse();
            Assert.fail("Expected AdalClaimsChallengeException was not thrown");
        } catch (AdalClaimsChallengeException ex) {
            Assert.assertEquals(claims.replace("\\", ""), ex.getClaims());
        }
        PowerMock.verifyAll();
    }

    @Test(expectedExceptions = SerializeException.class, expectedExceptionsMessageRegExp = "The endpoint URI is not specified")
    public void testNullUri() throws SerializeException, ParseException,
            AuthenticationException, IOException, java.text.ParseException,
            URISyntaxException {
        final ClientAuthentication ca = new ClientSecretPost(
                new ClientID("id"), new Secret("secret"));
        final AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        final MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag,
                (String) null);
        final ClientDataHttpHeaders cdhh = new ClientDataHttpHeaders("corr-id");
        final AdalTokenRequest request = new AdalTokenRequest(null, ca, grant,
                cdhh.getReadonlyHeaderMap(), null, null);
        Assert.assertNotNull(request);
        request.executeOAuthRequestAndProcessResponse();
    }

    @Test
    public void testConstructor() throws MalformedURLException,
            URISyntaxException {
        final ClientAuthentication ca = new ClientSecretPost(
                new ClientID("id"), new Secret("secret"));
        final AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        final MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag,
                (String) null);
        final ClientDataHttpHeaders cdhh = new ClientDataHttpHeaders("corr-id");
        final AdalTokenRequest request = new AdalTokenRequest(new URL(
                "http://login.windows.net"), ca, grant,
                cdhh.getReadonlyHeaderMap(), null, null);
        Assert.assertNotNull(request);
    }

    @Test
    public void testToOAuthRequestNonEmptyCorrelationId()
            throws MalformedURLException, SerializeException,
            URISyntaxException {
        final ClientAuthentication ca = new ClientSecretPost(
                new ClientID("id"), new Secret("secret"));
        final AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        final MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag,
                (String) null);
        final ClientDataHttpHeaders cdhh = new ClientDataHttpHeaders("corr-id");
        final AdalTokenRequest request = new AdalTokenRequest(new URL(
                "http://login.windows.net"), ca, grant,
                cdhh.getReadonlyHeaderMap(), null, null);
        Assert.assertNotNull(request);
        final AdalOAuthRequest req = request.toOAuthRequest();
        Assert.assertNotNull(req);
        Assert.assertEquals(
                "corr-id",
                cdhh.getReadonlyHeaderMap().get(
                        ClientDataHttpHeaders.CORRELATION_ID_HEADER_NAME));
    }

    @Test
    public void testToOAuthRequestNullCorrelationId_NullClientAuth()
            throws MalformedURLException, SerializeException,
            URISyntaxException {
        final AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        final MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag,
                (String) null);
        final AdalTokenRequest request = new AdalTokenRequest(new URL(
                "http://login.windows.net"), null, grant, null, null, null);
        Assert.assertNotNull(request);
        final AdalOAuthRequest req = request.toOAuthRequest();
        Assert.assertNotNull(req);
    }

    @Test
    public void testExecuteOAuth_Success() throws SerializeException,
            ParseException, AuthenticationException, IOException,
            java.text.ParseException, URISyntaxException {
        final AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        final MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag,
                (String) null);

        final AdalTokenRequest request = PowerMock.createPartialMock(
                AdalTokenRequest.class, new String[] { "toOAuthRequest" },
                new URL("http://login.windows.net"), null, grant, null, null, null);
        final AdalOAuthRequest adalOAuthHttpRequest = PowerMock
                .createMock(AdalOAuthRequest.class);
        final HTTPResponse httpResponse = PowerMock
                .createMock(HTTPResponse.class);
        EasyMock.expect(request.toOAuthRequest())
                .andReturn(adalOAuthHttpRequest).times(1);
        EasyMock.expect(adalOAuthHttpRequest.send()).andReturn(httpResponse)
                .times(1);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(200).times(1);
        EasyMock.expect(httpResponse.getContentAsJSONObject())
                .andReturn(
                        JSONObjectUtils
                                .parseJSONObject(TestConfiguration.HTTP_RESPONSE_FROM_AUTH_CODE))
                .times(1);
        httpResponse.ensureStatusCode(200);
        EasyMock.expectLastCall();

        PowerMock.replay(request, adalOAuthHttpRequest, httpResponse);

        final AuthenticationResult result = request
                .executeOAuthRequestAndProcessResponse();
        PowerMock.verifyAll();
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getUserInfo());
        Assert.assertFalse(StringHelper.isBlank(result.getAccessToken()));
        Assert.assertFalse(StringHelper.isBlank(result.getRefreshToken()));
        Assert.assertTrue(result.isMultipleResourceRefreshToken());
        Assert.assertEquals(result.getExpiresAfter(), 3600);
        Assert.assertEquals(result.getAccessTokenType(), "Bearer");
        Assert.assertEquals(result.getUserInfo().getFamilyName(), "Admin");
        Assert.assertEquals(result.getUserInfo().getGivenName(), "ADALTests");
        Assert.assertEquals(result.getUserInfo().getDisplayableId(),
                "admin@aaltests.onmicrosoft.com");
        Assert.assertNull(result.getUserInfo().getIdentityProvider());
    }

    @Test(expectedExceptions = AuthenticationException.class)
    public void testExecuteOAuth_Failure() throws SerializeException,
            ParseException, AuthenticationException, IOException,
            java.text.ParseException, URISyntaxException {
        final AuthorizationGrant ag = new AuthorizationCodeGrant(
                new AuthorizationCode("code"),
                new URI("http://my.redirect.com"));
        final MsalOAuthAuthorizationGrant grant = new MsalOAuthAuthorizationGrant(ag,
                (String) null);

        final AdalTokenRequest request = PowerMock.createPartialMock(
                AdalTokenRequest.class, new String[] { "toOAuthRequest" },
                new URL("http://login.windows.net"), null, grant, null, null, null);
        final AdalOAuthRequest adalOAuthHttpRequest = PowerMock
                .createMock(AdalOAuthRequest.class);
        final HTTPResponse httpResponse = PowerMock
                .createMock(HTTPResponse.class);
        EasyMock.expect(request.toOAuthRequest())
                .andReturn(adalOAuthHttpRequest).times(1);
        EasyMock.expect(adalOAuthHttpRequest.send()).andReturn(httpResponse)
                .times(1);
        EasyMock.expect(httpResponse.getStatusCode()).andReturn(402).times(1);

        final TokenErrorResponse errorResponse = PowerMock
                .createMock(TokenErrorResponse.class);

        final ErrorObject errorObject = PowerMock.createMock(ErrorObject.class);

        EasyMock.expect(errorObject.getCode())
                .andReturn("unknown").times(1);
        EasyMock.expect(errorObject.getHTTPStatusCode())
                .andReturn(402).times(1);

        EasyMock.expect(errorResponse.getErrorObject())
                .andReturn(errorObject).times(1);

        PowerMock.mockStaticPartial(TokenErrorResponse.class, "parse");
        PowerMock.createPartialMock(TokenErrorResponse.class, "parse");
        EasyMock.expect(TokenErrorResponse.parse(httpResponse))
                .andReturn(errorResponse).times(1);

        final JSONObject jsonObj = PowerMock.createMock(JSONObject.class);
        EasyMock.expect(jsonObj.toJSONString())
                .andReturn(TestConfiguration.HTTP_ERROR_RESPONSE).times(1);
        EasyMock.expect(errorResponse.toJSONObject()).andReturn(jsonObj)
                .times(1);

        PowerMock.replay(request, adalOAuthHttpRequest, httpResponse,
                TokenErrorResponse.class, errorObject, jsonObj, errorResponse);
        try {
            request.executeOAuthRequestAndProcessResponse();
            PowerMock.verifyAll();
        }
        finally {
            PowerMock.reset(request, adalOAuthHttpRequest, httpResponse,
                    TokenErrorResponse.class, jsonObj, errorResponse);
        }
    }
}
