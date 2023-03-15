// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
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
import java.util.*;

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

    AuthenticationResult executeTokenRequest() throws ParseException, IOException {

        log.debug("Sending token request to: " + requestAuthority.canonicalAuthorityUrl());
        OAuthHttpRequest oAuthHttpRequest = createOauthHttpRequest();
        HTTPResponse oauthHttpResponse = oAuthHttpRequest.send();
        return createAuthenticationResultFromOauthHttpResponse(oauthHttpResponse);
    }

    OAuthHttpRequest createOauthHttpRequest() throws SerializeException, MalformedURLException, ParseException {

        if (requestAuthority.tokenEndpointUrl() == null) {
            throw new SerializeException("The endpoint URI is not specified");
        }

        final OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
                HTTPRequest.Method.POST,
                requestAuthority.tokenEndpointUrl(),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.serviceBundle);
        oauthHttpRequest.setContentType(HTTPContentType.ApplicationURLEncoded.contentType);

        final Map<String, List<String>> params = new HashMap<>(msalRequest.msalAuthorizationGrant().toParameters());
        if (msalRequest.application().clientCapabilities() != null) {
            params.put("claims", Collections.singletonList(msalRequest.application().clientCapabilities()));
        }

        if (msalRequest.msalAuthorizationGrant.getClaims() != null) {
            String claimsRequest = msalRequest.msalAuthorizationGrant.getClaims().formatAsJSONString();
            if (params.get("claims") != null) {
                claimsRequest = JsonHelper.mergeJSONString(params.get("claims").get(0), claimsRequest);
            }
            params.put("claims", Collections.singletonList(claimsRequest));
        }

        if(msalRequest.requestContext().apiParameters().extraQueryParameters() != null ){
            for(String key: msalRequest.requestContext().apiParameters().extraQueryParameters().keySet()){
                    if(params.containsKey(key)){
                        throw new MsalClientException("Conflicting parameters","400 - Bad Request");
                    }
                    params.put(key, Collections.singletonList(msalRequest.requestContext().apiParameters().extraQueryParameters().get(key)));
            }
        }

        oauthHttpRequest.setQuery(URLUtils.serializeParameters(params));
      
        if (msalRequest.application().clientAuthentication() != null) {

            Map<String, List<String>> queryParameters = oauthHttpRequest.getQueryParameters();
            String clientID = msalRequest.application().clientId();
            queryParameters.put("client_id", Arrays.asList(clientID));
            oauthHttpRequest.setQuery(URLUtils.serializeParameters(queryParameters));

            // If the client application has a client assertion to apply to the request, check if a new client assertion
            //  was supplied as a request parameter. If so, use the request's assertion instead of the application's
            if (msalRequest instanceof ClientCredentialRequest && ((ClientCredentialRequest) msalRequest).parameters.clientCredential() != null) {
                ((ConfidentialClientApplication) msalRequest.application())
                        .createClientAuthFromClientAssertion((ClientAssertion) ((ClientCredentialRequest) msalRequest).parameters.clientCredential())
                        .applyTo(oauthHttpRequest);
            } else {
                msalRequest.application().clientAuthentication().applyTo(oauthHttpRequest);
            }
        }
        return oauthHttpRequest;
    }

    private AuthenticationResult createAuthenticationResultFromOauthHttpResponse(
            HTTPResponse oauthHttpResponse) throws ParseException {
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
                    if (type == AuthorityType.B2C) {
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
                } else if (type == AuthorityType.ADFS) {
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
            if (oauthHttpResponse.getStatusCode() == HttpHelper.HTTP_STATUS_429 || oauthHttpResponse.getStatusCode() >= HttpHelper.HTTP_STATUS_500) {
                serviceBundle.getServerSideTelemetry().previousRequests.putAll(
                        serviceBundle.getServerSideTelemetry().previousRequestInProgress);
            }

            throw MsalServiceExceptionFactory.fromHttpResponse(oauthHttpResponse);
        }
        return result;
    }
}