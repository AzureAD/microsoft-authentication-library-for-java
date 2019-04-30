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

import com.google.common.base.Strings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
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

    /**
     *
     * @return
     * @throws ParseException
     * @throws AuthenticationException
     * @throws SerializeException
     * @throws IOException
     */
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

                Account account = null;

                if (tokens.getIDToken() != null) {
                    String idTokenJson = tokens.getIDToken().getParsedParts()[1].decodeToString();
                    IdToken idToken = JsonHelper.convertJsonToObject(idTokenJson, IdToken.class);

                    if (!StringHelper.isBlank(response.getClientInfo())) {

                        AuthorityType type = msalRequest.application().authenticationAuthority.authorityType;
                        if(type == AuthorityType.B2C){

                            B2CAuthority authority = (B2CAuthority) msalRequest.application().authenticationAuthority;

                            account = Account.create(
                                    response.getClientInfo(),
                                    url.getHost(),
                                    idToken,
                                    authority.policy);
                        } else {
                            account = Account.create(
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
                        idToken(tokens.getIDTokenString()).
                        environment(url.getHost()).
                        expiresOn(currTimestampSec + response.getExpiresIn()).
                        extExpiresOn(response.getExtExpiresIn() > 0 ? currTimestampSec + response.getExtExpiresIn() : 0).
                        account(account).
                        scopes(response.getScope()).
                        build();

            } else {
                final TokenErrorResponse errorResponse = TokenErrorResponse.parse(httpResponse);
                ErrorObject errorObject = errorResponse.getErrorObject();

                if (AuthenticationErrorCode.AUTHORIZATION_PENDING.toString()
                        .equals(errorObject.getCode())) {

                    httpEvent.setOauthErrorCode(AuthenticationErrorCode.AUTHORIZATION_PENDING.toString());

                    throw new AuthenticationException(AuthenticationErrorCode.AUTHORIZATION_PENDING,
                            errorObject.getDescription());
                }

                if (HTTPResponse.SC_BAD_REQUEST == errorObject.getHTTPStatusCode() &&
                        AuthenticationErrorCode.INTERACTION_REQUIRED.toString().equals(errorObject.getCode())) {

                    httpEvent.setOauthErrorCode(AuthenticationErrorCode.INTERACTION_REQUIRED.toString());

                    throw new ClaimsChallengeException(
                            errorResponse.toJSONObject().toJSONString(),
                            getClaims(httpResponse.getContent()));
                } else {
                    String telemetryErrorCode = Strings.isNullOrEmpty(errorObject.getCode()) ?
                            AuthenticationErrorCode.UNKNOWN.toString() :
                            errorObject.getCode();

                    httpEvent.setOauthErrorCode(telemetryErrorCode);

                    throw new AuthenticationException(errorResponse.toJSONObject().toJSONString());
                }
            }
            return result;
        }
    }

    private void addResponseHeadersToHttpEvent(HttpEvent httpEvent, HTTPResponse httpResponse) {
        httpEvent.setHttpResponseStatus(httpResponse.getStatusCode());

        if (!Strings.isNullOrEmpty(httpResponse.getHeaderValue("User-Agent"))) {
            httpEvent.setUserAgent(httpResponse.getHeaderValue("User-Agent"));
        }

        if (!Strings.isNullOrEmpty(httpResponse.getHeaderValue("x-ms-request-id"))) {
            httpEvent.setRequestIdHeader(httpResponse.getHeaderValue("x-ms-request-id"));
        }

        if (!Strings.isNullOrEmpty(httpResponse.getHeaderValue("x-ms-clitelem"))) {
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
            if(!Strings.isNullOrEmpty(url.getQuery()))
                httpEvent.setQueryParameters(url.getQuery());
        } catch(URISyntaxException ex){
            log.warn(LogHelper.createMessage("Setting URL telemetry fields failed: " +
                            LogHelper.getPiiScrubbedDetails(ex),
                    msalRequest.headers().getHeaderCorrelationIdValue()));
        }
        return httpEvent;
    }

    private String getClaims(String httpResponseContentStr) {
        JsonElement root = new JsonParser().parse(httpResponseContentStr);

        JsonElement claims = root.getAsJsonObject().get("claims");

        return claims != null ? claims.getAsString() : null;
    }

    /**
     *
     * @return
     * @throws SerializeException
     */
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