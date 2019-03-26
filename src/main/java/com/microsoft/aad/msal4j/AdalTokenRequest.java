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
import java.util.Date;
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
     */
    AuthenticationResult executeOAuthRequestAndProcessResponse()
            throws ParseException, AuthenticationException, SerializeException, IOException {

        AuthenticationResult result;
        HTTPResponse httpResponse;
        final AdalOAuthRequest adalOAuthHttpRequest = this.toOAuthRequest();
        httpResponse = adalOAuthHttpRequest.send();

        if (httpResponse.getStatusCode() == HTTPResponse.SC_OK) {
            final MsalAccessTokenResponse response = MsalAccessTokenResponse.parseHttpResponse(httpResponse);

            OIDCTokens tokens = response.getOIDCTokens();
            String refreshToken = null;
            if (tokens.getRefreshToken() != null) {
                refreshToken = tokens.getRefreshToken().getValue();
            }

            Account account = null;

            if (tokens.getIDToken() != null) {
                String idTokenJson = tokens.getIDToken().getParsedParts()[1].decodeToString();
                IdToken idToken = JsonHelper.convertJsonToObject(idTokenJson, IdToken.class);

                if(!StringHelper.isBlank(response.getClientInfo())){
                    account = Account.create(response.getClientInfo(), uri.getHost(), idToken);
                }
            }
            long currTimestampSec = new Date().getTime() / 1000;

            result = AuthenticationResult.builder().
                        accessToken(tokens.getAccessToken().getValue()).
                        refreshToken(refreshToken).
                        idToken(tokens.getIDTokenString()).
                        environment(uri.getHost()).
                        expiresOn(currTimestampSec + response.getExpiresIn()).
                        extExpiresOn(response.getExtExpiresIn() > 0 ? currTimestampSec + response.getExtExpiresIn() : 0).
                        account(account).
                        scopes(response.getScope()).
                    build();
        } else {
            final TokenErrorResponse errorResponse = TokenErrorResponse
                    .parse(httpResponse);
            ErrorObject errorObject = errorResponse.getErrorObject();
            if(MsalErrorCode.AUTHORIZATION_PENDING.toString()
                    .equals(errorObject.getCode())){
                throw new AuthenticationException(MsalErrorCode.AUTHORIZATION_PENDING,
                        errorObject.getDescription());
            }

            if(HTTPResponse.SC_BAD_REQUEST == errorObject.getHTTPStatusCode() &&
                    MsalErrorCode.INTERACTION_REQUIRED.toString()
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

    public ClientAuthentication getClientAuth() {
        return clientAuth;
    }

    public AbstractMsalAuthorizationGrant getAuthorizationGrant() {
        return grant;
    }
}
