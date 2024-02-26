// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.JSONObjectUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenRequestExecutorTest {

    @Test
    void executeOAuthRequest_SCBadRequestErrorInvalidGrant_InteractionRequiredException()
            throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

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
        httpResponse.setContentType(HTTPContentType.ApplicationJSON.contentType);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();

        try {
            request.executeTokenRequest();
            fail("Expected MsalServiceException was not thrown");
        } catch (MsalInteractionRequiredException ex) {
            assertEquals(claims.replace("\\", ""), ex.claims());
            assertEquals(ex.reason(), InteractionRequiredExceptionReason.BASIC_ACTION);
        }
    }

    @Test
    void executeOAuthRequest_SCBadRequestErrorInvalidGrant_SubErrorFilteredServiceExceptionThrown()
            throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

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
        httpResponse.setContentType(HTTPContentType.ApplicationJSON.contentType);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();

        try {
            request.executeTokenRequest();
            fail("Expected MsalServiceException was not thrown");
        } catch (MsalServiceException ex) {
            assertEquals(claims.replace("\\", ""), ex.claims());
            assertTrue(!(ex instanceof MsalInteractionRequiredException));
        }
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
                new TelemetryManager(null, false),
                new DefaultHttpClient(null, null, null, null),
                new HttpHelper());

        return spy(new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), refreshTokenRequest, serviceBundle));
    }

    @Test
    void testConstructor() throws MalformedURLException,
            URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        final TokenRequestExecutor request = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null, null));
        assertNotNull(request);
    }

    @Test
    void testToOAuthRequestNonEmptyCorrelationId()
            throws MalformedURLException, SerializeException, URISyntaxException, ParseException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        AuthorizationCodeRequest acr = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        TokenRequestExecutor request = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null, null));
        assertNotNull(request);
        OAuthHttpRequest req = request.createOauthHttpRequest();
        assertNotNull(req);
        assertEquals(
                "corr-id",
                req.getExtraHeaderParams().get(HttpHeaders.CORRELATION_ID_HEADER_NAME));
    }

    @Test
    void testToOAuthRequestNullCorrelationId_NullClientAuth()
            throws MalformedURLException, SerializeException,
            URISyntaxException, ParseException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        final TokenRequestExecutor request = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                acr,
                new ServiceBundle(null, null, null, null));
        assertNotNull(request);
        final OAuthHttpRequest req = request.createOauthHttpRequest();
        assertNotNull(req);
    }

    @Test
    void testExecuteOAuth_Success() throws SerializeException, ParseException, MsalException,
            IOException, URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                new TelemetryManager(null, false),
                null,
                null);

        final TokenRequestExecutor request = spy(new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle));

        final OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

        final HTTPResponse httpResponse = mock(HTTPResponse.class);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();
        doReturn(JSONObjectUtils.parse(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE)).when(httpResponse).getContentAsJSONObject();

        httpResponse.ensureStatusCode(200);

        doReturn(200).when(httpResponse).getStatusCode();

        final AuthenticationResult result = request.executeTokenRequest();

        assertNotNull(result.account());
        assertNotNull(result.account().homeAccountId());
        assertEquals(result.account().username(), "idlab@msidlab4.onmicrosoft.com");

        assertFalse(StringHelper.isBlank(result.accessToken()));
        assertFalse(StringHelper.isBlank(result.refreshToken()));
    }

    @Test
    void testExecuteOAuth_Failure() throws SerializeException,
            ParseException, MsalException, IOException, URISyntaxException {

        PublicClientApplication app = PublicClientApplication.builder("id").correlationId("corr-id").build();

        AuthorizationCodeParameters parameters = AuthorizationCodeParameters
                .builder("code", new URI("http://my.redirect.com"))
                .scopes(Collections.singleton("default-scope"))
                .build();

        final AuthorizationCodeRequest acr = new AuthorizationCodeRequest(
                parameters,
                app,
                new RequestContext(app, PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE, parameters));

        ServiceBundle serviceBundle = new ServiceBundle(
                null,
                new TelemetryManager(null, false),
                null,
                null);

        final TokenRequestExecutor request = spy(new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle));
        final OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

        final HTTPResponse httpResponse = mock(HTTPResponse.class);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();
        lenient().doReturn(402).when(httpResponse).getStatusCode();
        doReturn("403 Forbidden").when(httpResponse).getStatusMessage();
        doReturn(new HashMap<>()).when(httpResponse).getHeaderMap();
        doReturn(TestConfiguration.HTTP_ERROR_RESPONSE).when(httpResponse).getContent();

        final ErrorResponse errorResponse = mock(ErrorResponse.class);

        lenient().doReturn("invalid_request").when(errorResponse).error();
        lenient().doReturn(null).when(httpResponse).getHeaderValue("User-Agent");
        lenient().doReturn(null).when(httpResponse).getHeaderValue("x-ms-request-id");
        lenient().doReturn(null).when(httpResponse).getHeaderValue("x-ms-clitelem");
        doReturn(402).when(httpResponse).getStatusCode();

        assertThrows(MsalException.class, request::executeTokenRequest);
    }
}