// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.*;

class TokenRequestExecutor {
    Logger log = LoggerFactory.getLogger(TokenRequestExecutor.class);

    final Authority requestAuthority;
    final String tenant;
    private final MsalRequest msalRequest;
    private final ServiceBundle serviceBundle;

    TokenRequestExecutor(Authority requestAuthority, MsalRequest msalRequest, ServiceBundle serviceBundle) {
        this.requestAuthority = requestAuthority;
        this.serviceBundle = serviceBundle;
        this.msalRequest = msalRequest;
        this.tenant = msalRequest.requestContext().apiParameters().tenant() == null ?
                msalRequest.application().tenant() :
                msalRequest.requestContext().apiParameters().tenant() ;
    }

    AuthenticationResult executeTokenRequest() throws ParseException, IOException {

        log.debug("Sending token request to: {}", requestAuthority.canonicalAuthorityUrl());
        OAuthHttpRequest oAuthHttpRequest = createOauthHttpRequest();
        HttpResponse oauthHttpResponse = oAuthHttpRequest.send();
        return createAuthenticationResultFromOauthHttpResponse(oauthHttpResponse);
    }

    OAuthHttpRequest createOauthHttpRequest() throws SerializeException, MalformedURLException {

        if (requestAuthority.tokenEndpointUrl() == null) {
            throw new SerializeException("The endpoint URI is not specified");
        }

        final OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
                HttpMethod.POST,
                requestAuthority.tokenEndpointUrl(),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.serviceBundle);

        final Map<String, List<String>> params = new HashMap<>(msalRequest.msalAuthorizationGrant().toParameters());
        if (msalRequest.application() instanceof AbstractClientApplicationBase
                && ((AbstractClientApplicationBase) msalRequest.application()).clientCapabilities() != null) {
            params.put("claims", Collections.singletonList(((AbstractClientApplicationBase) msalRequest.application()).clientCapabilities()));
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
                       log.warn("A query parameter {} has been provided with values multiple times.", key);
                    }
                    params.put(key, Collections.singletonList(msalRequest.requestContext().apiParameters().extraQueryParameters().get(key)));
            }
        }

        oauthHttpRequest.setQuery(URLUtils.serializeParameters(params));

        //Certain query parameters are required by Public and Confidential client applications, but not Managed Identity
        if (msalRequest.application() instanceof AbstractClientApplicationBase) {
            addQueryParameters(oauthHttpRequest);
        }
        return oauthHttpRequest;
    }

    private void addQueryParameters(OAuthHttpRequest oauthHttpRequest) {
        Map<String, List<String>> queryParameters = URLUtils.parseParameters(oauthHttpRequest.query);
        String clientID = msalRequest.application().clientId();
        queryParameters.put("client_id", Arrays.asList(clientID));

        // If the client application has a client assertion to apply to the request, check if a new client assertion
        //  was supplied as a request parameter. If so, use the request's assertion instead of the application's
        if (msalRequest.application() instanceof ConfidentialClientApplication) {
            if (msalRequest instanceof ClientCredentialRequest && ((ClientCredentialRequest) msalRequest).parameters.clientCredential() != null) {
                IClientCredential credential = ((ClientCredentialRequest) msalRequest).parameters.clientCredential();
                addJWTBearerAssertionParams(queryParameters, ((ConfidentialClientApplication) msalRequest.application()).getAssertionString(credential));
            } else {
                if (((ConfidentialClientApplication) msalRequest.application()).assertion != null) {
                    addJWTBearerAssertionParams(queryParameters, ((ConfidentialClientApplication) msalRequest.application()).assertion);
                } else if (((ConfidentialClientApplication) msalRequest.application()).secret != null) {
                    // Client secrets have a different parameter than bearer assertions
                    queryParameters.put("client_secret", Collections.singletonList(((ConfidentialClientApplication) msalRequest.application()).secret));
                }
            }
        }

        oauthHttpRequest.setQuery(URLUtils.serializeParameters(queryParameters));
    }

    private void addJWTBearerAssertionParams(Map<String, List<String>> queryParameters, String assertion) {
        queryParameters.put("client_assertion", Collections.singletonList(assertion));
        queryParameters.put("client_assertion_type", Collections.singletonList("urn:ietf:params:oauth:client-assertion-type:jwt-bearer"));
    }

    private AuthenticationResult createAuthenticationResultFromOauthHttpResponse(
            HttpResponse oauthHttpResponse) throws ParseException {
        AuthenticationResult result;

        if (oauthHttpResponse.statusCode() == HttpHelper.HTTP_STATUS_200) {
            final TokenResponse response = TokenResponse.parseHttpResponse(oauthHttpResponse);

            AccountCacheEntity accountCacheEntity = null;
            if (!StringHelper.isNullOrBlank(response.idToken())) {
                String idTokenJson;
                try {
                    idTokenJson = new String(Base64.getDecoder().decode(response.idToken().split("\\.")[1]), StandardCharsets.UTF_8);
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new MsalServiceException("Error parsing ID token, missing payload section. Ensure that the ID token is following the JWT format.",
                            AuthenticationErrorCode.INVALID_JWT);
                }

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
                    accessToken(response.accessToken()).
                    refreshToken(response.refreshToken()).
                    familyId(response.getFoci()).
                    idToken(response.idToken()).
                    environment(requestAuthority.host()).
                    expiresOn(currTimestampSec + response.getExpiresIn()).
                    extExpiresOn(response.getExtExpiresIn() > 0 ? currTimestampSec + response.getExtExpiresIn() : 0).
                    refreshOn(response.getRefreshIn() > 0 ? currTimestampSec + response.getRefreshIn() : 0).
                    accountCacheEntity(accountCacheEntity).
                    scopes(response.getScope()).
                    metadata(AuthenticationResultMetadata.builder()
                            .tokenSource(TokenSource.IDENTITY_PROVIDER)
                            .refreshOn(response.getRefreshIn() > 0 ? currTimestampSec + response.getRefreshIn() : 0)
                            .build()).
                    build();

        } else {
            // http codes indicating that STS did not log request
            if (oauthHttpResponse.statusCode() == HttpHelper.HTTP_STATUS_429 || oauthHttpResponse.statusCode() >= HttpHelper.HTTP_STATUS_500) {
                serviceBundle.getServerSideTelemetry().previousRequests.putAll(
                        serviceBundle.getServerSideTelemetry().previousRequestInProgress);
            }

            throw MsalServiceExceptionFactory.fromHttpResponse(oauthHttpResponse);
        }
        return result;
    }

    Logger getLog() {
        return this.log;
    }

    Authority getRequestAuthority() {
        return this.requestAuthority;
    }

    String getTenant() {
        return this.tenant;
    }

    MsalRequest getMsalRequest() {
        return this.msalRequest;
    }

    ServiceBundle getServiceBundle() {
        return this.serviceBundle;
    }
}