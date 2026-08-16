// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.SSLSocketFactory;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TokenRequestExecutorTest {

    @Test
    void managedIdentityMtlsRequestUsesNormalClaimsPipelineAndSocketFactory()
            throws Exception {
        ManagedIdentityApplication app = ManagedIdentityApplication
                .builder(ManagedIdentityId.systemAssigned())
                .clientCapabilities(Collections.singletonList("cp1"))
                .build();
        ManagedIdentityParameters parameters = ManagedIdentityParameters
                .builder("https://vault.azure.net")
                .claims("{\"access_token\":{\"custom\":{\"essential\":true}}}")
                .withMtlsProofOfPossession()
                .build();
        ManagedIdentityRequest managedIdentityRequest =
                new ManagedIdentityRequest(
                        app,
                        new RequestContext(
                                app,
                                PublicApi.ACQUIRE_TOKEN_BY_SYSTEM_ASSIGNED_MANAGED_IDENTITY,
                                parameters));
        TokenRequestExecutor executor = new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)),
                managedIdentityRequest,
                app.serviceBundle());
        SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("token_type", "mtls_pop");

        OAuthHttpRequest request = executor.createOauthHttpRequest(
                new URL("https://login.example/tenant/oauth2/v2.0/token"),
                socketFactory,
                body);
        Map<String, String> query = StringHelper.parseQueryParameters(request.query);

        assertEquals(socketFactory, request.sslSocketFactory());
        assertEquals("mtls_pop", query.get("token_type"));
        assertTrue(query.get("claims").contains("\"xms_cc\""));
        assertTrue(query.get("claims").contains("\"custom\""));
    }

    @Test
    void executeOAuthRequest_SCBadRequestErrorInvalidGrant_InteractionRequiredException()
            throws MsalException,
            IOException, URISyntaxException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.statusCode(HttpStatus.HTTP_BAD_REQUEST);

        String claims = "{\\\"access_token\\\":{\\\"polids\\\":{\\\"essential\\\":true,\\\"values\\\":[\\\"5ce770ea-8690-4747-aa73-c5b3cd509cd4\\\"]}}}";

        String content = "{\"error\":\"invalid_grant\"," +
                "\"error_description\":\"AADSTS65001: description\\r\\nCorrelation ID: 3a...5a\\r\\nTimestamp:2017-07-15 02:35:05Z\"," +
                "\"error_codes\":[50076]," +
                "\"timestamp\":\"2017-07-15 02:35:05Z\"," +
                "\"trace_id\":\"0788...000\"," +
                "\"correlation_id\":\"3a...95a\"," +
                "\"suberror\":\"basic_action\"," +
                "\"claims\":\"" + claims + "\"}";
        httpResponse.body(content);
        httpResponse.addHeader("Content-Type", HTTPContentType.ApplicationJSON.contentType);

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
            throws MsalException,
            IOException, URISyntaxException {

        TokenRequestExecutor request = createMockedTokenRequest();

        OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

        HttpResponse httpResponse = new HttpResponse();
        httpResponse.statusCode(HttpStatus.HTTP_BAD_REQUEST);

        String claims = "{\\\"access_token\\\":{\\\"polids\\\":{\\\"essential\\\":true,\\\"values\\\":[\\\"5ce770ea-8690-4747-aa73-c5b3cd509cd4\\\"]}}}";

        String content = "{\"error\":\"invalid_grant\"," +
                "\"error_description\":\"AADSTS65001: description\\r\\nCorrelation ID: 3a...5a\\r\\nTimestamp:2017-07-15 02:35:05Z\"," +
                "\"error_codes\":[50076]," +
                "\"timestamp\":\"2017-07-15 02:35:05Z\"," +
                "\"trace_id\":\"0788...000\"," +
                "\"correlation_id\":\"3a...95a\"," +
                "\"suberror\":\"client_mismatch\"," +
                "\"claims\":\"" + claims + "\"}";
        httpResponse.body(content);
        httpResponse.addHeader("Content-Type", HTTPContentType.ApplicationJSON.contentType);

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

    private TokenRequestExecutor createMockedTokenRequest() throws MalformedURLException {
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
                new HttpHelper(new DefaultHttpClient(null, null, null, null), new DefaultRetryPolicy()));

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
                new ServiceBundle(null, null,  null));
        assertNotNull(request);
    }

    @Test
    void testToOAuthRequestNonEmptyCorrelationId()
            throws MalformedURLException, URISyntaxException {

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
                new ServiceBundle(null, null, null));
        assertNotNull(request);
        OAuthHttpRequest req = request.createOauthHttpRequest();
        assertNotNull(req);
        assertEquals(
                "corr-id",
                req.getExtraHeaderParams().get(HttpHeaders.CORRELATION_ID_HEADER_NAME));
    }

    @Test
    void testToOAuthRequestNullCorrelationId_NullClientAuth() throws MalformedURLException, URISyntaxException {

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
                new ServiceBundle(null, null, null));
        assertNotNull(request);
        final OAuthHttpRequest req = request.createOauthHttpRequest();
        assertNotNull(req);
    }

    @Test
    void testExecuteOAuth_Success() throws MsalException, IOException, URISyntaxException {

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
                null);

        final TokenRequestExecutor request = spy(new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle));

        final OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

        final HttpResponse httpResponse = mock(HttpResponse.class);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();
        doReturn(JsonHelper.convertJsonToMap(TestConfiguration.TOKEN_ENDPOINT_OK_RESPONSE_ID_AND_ACCESS)).when(httpResponse).getBodyAsMap();

        doReturn(HttpStatus.HTTP_OK).when(httpResponse).statusCode();

        final AuthenticationResult result = request.executeTokenRequest();

        assertNotNull(result.account());
        assertNotNull(result.account().homeAccountId());
        assertEquals(result.account().username(), "idlab@msidlab4.onmicrosoft.com");

        assertFalse(StringHelper.isBlank(result.accessToken()));
        assertFalse(StringHelper.isBlank(result.refreshToken()));
    }

    @Test
    void testExecuteOAuth_Failure() throws MsalException, IOException, URISyntaxException {

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
                null);

        final TokenRequestExecutor request = spy(new TokenRequestExecutor(
                new AADAuthority(new URL(TestConstants.ORGANIZATIONS_AUTHORITY)), acr, serviceBundle));
        final OAuthHttpRequest msalOAuthHttpRequest = mock(OAuthHttpRequest.class);

        final HttpResponse httpResponse = mock(HttpResponse.class);

        doReturn(msalOAuthHttpRequest).when(request).createOauthHttpRequest();
        doReturn(httpResponse).when(msalOAuthHttpRequest).send();
        doReturn(TestConfiguration.HTTP_ERROR_RESPONSE).when(httpResponse).body();

        doReturn(402).when(httpResponse).statusCode();

        assertThrows(MsalException.class, request::executeTokenRequest);
    }

    @Test
    void testBase64UrlEncoding() throws MalformedURLException, ExecutionException, InterruptedException {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        //ID token payloads are parsed to get certain info to create Account and AccountCacheEntity objects, and the library must decode them using a Base64URL decoder.
        HashMap<String, String> tokenParameters = new HashMap<>();
        tokenParameters.put("preferred_username", "~nameWith~specialChars");
        String encodedIDToken = TestHelper.createIdToken(tokenParameters);
        try {
            //TestHelper.createIdToken() should use Base64URL encoding, so first we prove that the encoded token it produces cannot be decoded with Base64 decoder
            Base64.getDecoder().decode(encodedIDToken.split("\\.")[1]);

            fail("IllegalArgumentException was expected but not thrown.");
        } catch (IllegalArgumentException e) {
            //Encoded token should have some "-" characters in it
            assertTrue(e.getMessage().contains("Illegal base64 character 2d"));
        }

        //Now, send that encoded token through the library's token request flow, which will decode it using a Base64URL decoder
        HashMap<String, String> responseParameters = new HashMap<>();
        responseParameters.put("id_token", encodedIDToken);
        responseParameters.put("access_token", "token");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), HttpStatus.HTTP_OK);

        OnBehalfOfParameters parameters = OnBehalfOfParameters.builder(Collections.singleton("someScopes"), new UserAssertion(TestHelper.signedAssertion)).build();
        IAuthenticationResult result = cca.acquireToken(parameters).get();

        //Ensure that the name was successfully parsed out of the encoded ID token
        assertNotNull(result.idToken());
        assertNotNull(result.account());
        assertEquals("~nameWith~specialChars", result.account().username());
    }
}