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

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

import com.nimbusds.oauth2.sdk.ErrorObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenErrorResponse;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;

/**
 * Extension for TokenRequest to support additional header values like
 * correlation id.
 */
class AdalTokenRequest {

    private final URL uri;
    private final ClientAuthentication clientAuth;
    private final AbstractMsalAuthorizationGrant grant;
    private final Map<String, String> headerMap;
    private final Proxy proxy;
    private final SSLSocketFactory sslSocketFactory;

    AdalTokenRequest(final URL uri, final ClientAuthentication clientAuth,
            final AbstractMsalAuthorizationGrant authzGrant,
            final Map<String, String> headerMap, final Proxy proxy,
            final SSLSocketFactory sslSocketFactory) {
        this.clientAuth = clientAuth;
        this.grant = authzGrant;
        this.uri = uri;
        this.headerMap = headerMap;
        this.proxy = proxy;
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     *
     * @return
     * @throws ParseException
     * @throws AuthenticationException
     * @throws SerializeException
     * @throws IOException
     * @throws java.text.ParseException
     */
    AuthenticationResult executeOAuthRequestAndProcessResponse()
            throws ParseException, AuthenticationException, SerializeException,
            IOException, java.text.ParseException {

        AuthenticationResult result = null;
        HTTPResponse httpResponse = null;
        final AdalOAuthRequest adalOAuthHttpRequest = this.toOAuthRequest();
        httpResponse = adalOAuthHttpRequest.send();

        if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
            final AdalAccessTokenResponse response = AdalAccessTokenResponse
                    .parseHttpResponse(httpResponse);

            OIDCTokens tokens = response.getOIDCTokens();
            String refreshToken = null;
            if (tokens.getRefreshToken() != null) {
                refreshToken = tokens.getRefreshToken().getValue();
            }

            UserInfo info = null;
            if (tokens.getIDToken() != null) {
                info = UserInfo.createFromIdTokenClaims(tokens.getIDToken()
                        .getJWTClaimsSet());
            }

            result = new AuthenticationResult(tokens.getAccessToken()
                    .getType().getValue(),
                    tokens.getAccessToken().getValue(), refreshToken,
                    tokens.getAccessToken().getLifetime(),
                    tokens.getIDTokenString(), info,
                    !StringHelper.isBlank(response.getScope()));
        } else {
            final TokenErrorResponse errorResponse = TokenErrorResponse
                    .parse(httpResponse);
            ErrorObject errorObject = errorResponse.getErrorObject();
            if(AdalErrorCode.AUTHORIZATION_PENDING.toString()
                    .equals(errorObject.getCode())){
                throw new AuthenticationException(AdalErrorCode.AUTHORIZATION_PENDING,
                        errorObject.getDescription());
            }

            if(HTTPResponse.SC_BAD_REQUEST == errorObject.getHTTPStatusCode() &&
                    AdalErrorCode.INTERACTION_REQUIRED.toString()
                            .equals(errorObject.getCode())){
                throw new AdalClaimsChallengeException(errorResponse.toJSONObject()
                        .toJSONString(), getClaims(httpResponse.getContent()));
            }
            else {
                throw new AuthenticationException(errorResponse.toJSONObject()
                        .toJSONString());
            }
        }

        return result;
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
    AdalOAuthRequest toOAuthRequest() throws SerializeException {

        if (this.uri == null) {
            throw new SerializeException("The endpoint URI is not specified");
        }

        final AdalOAuthRequest httpRequest = new AdalOAuthRequest(
                HTTPRequest.Method.POST, this.uri, headerMap, this.proxy,
                this.sslSocketFactory);
        httpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);
        final Map<String, String> params = this.grant.toParameters();
        httpRequest.setQuery(URLUtils.serializeParameters(params));
        if (this.clientAuth != null) {
            this.clientAuth.applyTo(httpRequest);
        }

        return httpRequest;
    }
}
