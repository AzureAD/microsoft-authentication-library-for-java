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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.AccessLevel;
import lombok.Getter;

@Getter(AccessLevel.PACKAGE)
class TokenRequest {
    Logger log = LoggerFactory.getLogger(TokenRequest.class);

    private final URL url;
    private final MsalRequest msalRequest;
    private final ServiceBundle serviceBundle;

    TokenRequest(final URL url, MsalRequest msalRequest, final ServiceBundle serviceBundle) {
        this.url = url;
        this.serviceBundle = serviceBundle;
        this.msalRequest = msalRequest;
    }

    AuthenticationResult executeOauthRequestAndProcessResponse()
            throws ParseException, AuthenticationException, SerializeException,
            IOException {

        HttpEvent httpEvent = createHttpEvent();

        try(TelemetryHelper telemetryHelper = serviceBundle.getTelemetryManager().createTelemetryHelper(
                msalRequest.requestContext().getTelemetryRequestId(),
                msalRequest.application().clientId(),
                httpEvent,
                false)) {

            AuthenticationResult result;
            HTTPResponse httpResponse;

            httpResponse = toOauthHttpRequest().send();

            addResponseHeadersToHttpEvent(httpEvent, httpResponse);

            if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
                final TokenResponse response =
                        TokenResponse.parseHttpResponse(httpResponse);

                OIDCTokens tokens = response.getOIDCTokens();
                String refreshToken = null;
                if (tokens.getRefreshToken() != null) {
                    refreshToken = tokens.getRefreshToken().getValue();
                }

                AccountCacheEntity accountCacheEntity = null;

                if (tokens.getIDToken() != null) {
                    String idTokenJson = tokens.getIDToken().getParsedParts()[1].decodeToString();
                    IdToken idToken = JsonHelper.convertJsonToObject(idTokenJson, IdToken.class);

                    if (!StringHelper.isBlank(response.getClientInfo())) {

                        AuthorityType type = msalRequest.application().authenticationAuthority.authorityType;
                        if(type == AuthorityType.B2C){

                            B2CAuthority authority = (B2CAuthority) msalRequest.application().authenticationAuthority;

                            accountCacheEntity = AccountCacheEntity.create(
                                    response.getClientInfo(),
                                    url.getHost(),
                                    idToken,
                                    authority.policy);
                        } else {
                            accountCacheEntity = AccountCacheEntity.create(
                                    response.getClientInfo(),
                                    url.getHost(),
                                    idToken);
                        }
                    }
                }
                long currTimestampSec = new Date().getTime() / 1000;

                result = AuthenticationResult.builder().
                        accessToken(tokens.getAccessToken().getValue()).
                        refreshToken(refreshToken).
                        familyId(response.getFoci()).
                        idToken(tokens.getIDTokenString()).
                        environment(url.getHost()).
                        expiresOn(currTimestampSec + response.getExpiresIn()).
                        extExpiresOn(response.getExtExpiresIn() > 0 ? currTimestampSec + response.getExtExpiresIn() : 0).
                        accountCacheEntity(accountCacheEntity).
                        scopes(response.getScope()).
                        build();

            } else {

                String responseContent = httpResponse.getContent();
                if(responseContent == null || StringHelper.isBlank(responseContent)){
                    throw new AuthenticationServiceException("Unknown Service Exception");
                }

                ErrorResponse errorResponse = JsonHelper.convertJsonToObject(
                        responseContent,
                        ErrorResponse.class);

                errorResponse.statusCode(httpResponse.getStatusCode());
                errorResponse.statusMessage(httpResponse.getStatusMessage());

                // Some invalid_grant or interaction_required subError codes returned by
                // the service are not supposed to be exposed to customers
                if(errorResponse.error() != null &&
                        errorResponse.error().equalsIgnoreCase(AuthenticationErrorCode.INVALID_GRANT) ||
                        errorResponse.error().equalsIgnoreCase(AuthenticationErrorCode.INTERACTION_REQUIRED)){
                    errorResponse = filterSubErrorCode(errorResponse);
                }

                httpEvent.setOauthErrorCode(errorResponse.error());

                throw new AuthenticationServiceException(
                        errorResponse,
                        httpResponse.getHeaderMap());
            }
            return result;
        }
    }

    private ErrorResponse filterSubErrorCode(ErrorResponse errorResponse){
        String[] errorsThatShouldNotBeExposed = {"bad_token", "token_expired",
                "protection_policy_required", "client_mismatch", "device_authentication_failed"};

        Set<String> set = new HashSet<>(Arrays.asList(errorsThatShouldNotBeExposed));
        
        if(set.contains(errorResponse.subError)){
            errorResponse.subError("");
        }

        return errorResponse;
    }

    private void addResponseHeadersToHttpEvent(HttpEvent httpEvent, HTTPResponse httpResponse) {
        httpEvent.setHttpResponseStatus(httpResponse.getStatusCode());

        if (!StringHelper.isBlank(httpResponse.getHeaderValue("User-Agent"))) {
            httpEvent.setUserAgent(httpResponse.getHeaderValue("User-Agent"));
        }

        if (!StringHelper.isBlank(httpResponse.getHeaderValue("x-ms-request-id"))) {
            httpEvent.setRequestIdHeader(httpResponse.getHeaderValue("x-ms-request-id"));
        }

        if (!StringHelper.isBlank(httpResponse.getHeaderValue("x-ms-clitelem"))) {
            XmsClientTelemetryInfo xmsClientTelemetryInfo =
                    XmsClientTelemetryInfo.parseXmsTelemetryInfo(
                            httpResponse.getHeaderValue("x-ms-clitelem"));
            if (xmsClientTelemetryInfo != null) {
                httpEvent.setXmsClientTelemetryInfo(xmsClientTelemetryInfo);
            }
        }
    }

    private HttpEvent createHttpEvent() {
        HttpEvent httpEvent = new HttpEvent();
        httpEvent.setHttpMethod("POST");
        try {
            httpEvent.setHttpPath(url.toURI());
            if(!StringHelper.isBlank(url.getQuery()))
                httpEvent.setQueryParameters(url.getQuery());
        } catch(URISyntaxException ex){
            log.warn(LogHelper.createMessage("Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    msalRequest.headers().getHeaderCorrelationIdValue()));
        }
        return httpEvent;
    }

    OAuthHttpRequest toOauthHttpRequest() throws SerializeException {

        if (this.url == null) {
            throw new SerializeException("The endpoint URI is not specified");
        }

        final OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
                HTTPRequest.Method.POST,
                this.url,
                msalRequest.headers().getReadonlyHeaderMap(),
                this.serviceBundle);
        oauthHttpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);

        final Map<String, List<String>> params = msalRequest.msalAuthorizationGrant().toParameters();

        oauthHttpRequest.setQuery(URLUtils.serializeParameters(params));

        if (msalRequest.application().clientAuthentication != null) {
            msalRequest.application().clientAuthentication.applyTo(oauthHttpRequest);
        }

        return oauthHttpRequest;
    }
}