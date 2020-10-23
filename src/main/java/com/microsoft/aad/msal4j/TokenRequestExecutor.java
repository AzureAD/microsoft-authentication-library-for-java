// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.nimbusds.openid.connect.sdk.token.OIDCTokens;
import lombok.AccessLevel;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter(AccessLevel.PACKAGE)
class TokenRequestExecutor {
    Logger log = LoggerFactory.getLogger(TokenRequestExecutor.class);

    final Authority requestAuthority;
    private final MsalRequest msalRequest;
    private final ServiceBundle serviceBundle;

    TokenRequestExecutor(Authority requestAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) {
        this.requestAuthority = requestAuthority;
        this.serviceBundle = serviceBundle;
        this.msalRequest = msalRequest;
    }

    AuthenticationResult executeTokenRequest() throws ParseException,
            MsalServiceException, SerializeException, IOException {

        OAuthHttpRequest oAuthHttpRequest = createOauthHttpRequest();
        HTTPResponse oauthHttpResponse = oAuthHttpRequest.send();
        return createAuthenticationResultFromOauthHttpResponse(oauthHttpResponse);
    }

    OAuthHttpRequest createOauthHttpRequest() throws SerializeException, MalformedURLException {

        if (requestAuthority.tokenEndpointUrl() == null) {
            throw new SerializeException("The endpoint URI is not specified");
        }

        final OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
                HTTPRequest.Method.POST,
                requestAuthority.tokenEndpointUrl(),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.serviceBundle);
        oauthHttpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);

        final Map<String, List<String>> params = new HashMap<>(msalRequest.msalAuthorizationGrant().toParameters());
        if (msalRequest.application().clientCapabilities() != null) {
            params.put("claims", Collections.singletonList(msalRequest.application().clientCapabilities()));
        }

        oauthHttpRequest.setQuery(URLUtils.serializeParameters(params));

        if (msalRequest.application().clientAuthentication() != null) {
            msalRequest.application().clientAuthentication().applyTo(oauthHttpRequest);
        }
        return oauthHttpRequest;
    }

    private AuthenticationResult createAuthenticationResultFromOauthHttpResponse(
            HTTPResponse oauthHttpResponse) throws ParseException{
        AuthenticationResult result;

        if (oauthHttpResponse.getStatusCode() == HTTPResponse.SC_OK) {
            final TokenResponse response = TokenResponse.parseHttpResponse(oauthHttpResponse);

            OIDCTokens tokens = response.getOIDCTokens();
            String refreshToken = null;
            if (tokens.getRefreshToken() != null) {
                refreshToken = tokens.getRefreshToken().getValue();
            }

            AccountCacheEntity accountCacheEntity = null;

            if (tokens.getIDToken() != null) {
                String idTokenJson = tokens.getIDToken().getParsedParts()[1].decodeToString();
                IdToken idToken = JsonHelper.convertJsonToObject(idTokenJson, IdToken.class);

                AuthorityType type = msalRequest.application().authenticationAuthority.authorityType;
                if (!StringHelper.isBlank(response.getClientInfo())) {
                    if(type == AuthorityType.B2C){
                        B2CAuthority authority = (B2CAuthority) msalRequest.application().authenticationAuthority;
                        accountCacheEntity = AccountCacheEntity.create(
                                response.getClientInfo(),
                                requestAuthority,
                                idToken,
                                authority.policy());
                    } else {
                        accountCacheEntity = AccountCacheEntity.create(
                                response.getClientInfo(),
                                requestAuthority,
                                idToken);
                    }
                }
                else if(type == AuthorityType.ADFS){
                    accountCacheEntity = AccountCacheEntity.createADFSAccount(requestAuthority, idToken);
                }
            }
            long currTimestampSec = new Date().getTime() / 1000;

            result = AuthenticationResult.builder().
                    accessToken(tokens.getAccessToken().getValue()).
                    refreshToken(refreshToken).
                    familyId(response.getFoci()).
                    idToken(tokens.getIDTokenString()).
                    environment(requestAuthority.host()).
                    expiresOn(currTimestampSec + response.getExpiresIn()).
                    extExpiresOn(response.getExtExpiresIn() > 0 ? currTimestampSec + response.getExtExpiresIn() : 0).
                    refreshOn(response.getRefreshIn() > 0 ? currTimestampSec + response.getRefreshIn() : 0).
                    accountCacheEntity(accountCacheEntity).
                    scopes(response.getScope()).
                    build();

        } else {
            // http codes indicating that STS did not log request
            if(oauthHttpResponse.getStatusCode() == 429 || oauthHttpResponse.getStatusCode() >= 500){
                serviceBundle.getServerSideTelemetry().previousRequests.putAll(
                        serviceBundle.getServerSideTelemetry().previousRequestInProgress);
            }

            throw MsalServiceExceptionFactory.fromHttpResponse(oauthHttpResponse);
        }
        return result;
    }
}