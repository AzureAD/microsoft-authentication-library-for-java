// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

class TokenRequestExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(TokenRequestExecutor.class);

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

    AuthenticationResult executeTokenRequest() throws IOException {

        LOG.debug("Sending token request to: {}", requestAuthority.canonicalAuthorityUrl());
        OAuthHttpRequest oAuthHttpRequest = createOauthHttpRequest();
        HttpResponse oauthHttpResponse = oAuthHttpRequest.send();
        return createAuthenticationResultFromOauthHttpResponse(oauthHttpResponse);
    }

    AuthenticationResult executeTokenRequest(
            URL tokenEndpoint,
            SSLSocketFactory sslSocketFactory,
            Map<String, String> parameters) throws IOException {
        LOG.debug("Sending token request to: {}", tokenEndpoint);
        OAuthHttpRequest request = createOauthHttpRequest(
                tokenEndpoint,
                sslSocketFactory,
                parameters);
        return createAuthenticationResultFromOauthHttpResponse(request.send());
    }

    AuthenticationResult executeTokenRequest(
            URL tokenEndpoint,
            SSLContext sslContext,
            Map<String, String> parameters) throws IOException {
        LOG.debug("Sending token request to: {}", tokenEndpoint);
        OAuthHttpRequest request = createOauthHttpRequest(
                tokenEndpoint,
                sslContext,
                parameters);
        return createAuthenticationResultFromOauthHttpResponse(request.send());
    }

    OAuthHttpRequest createOauthHttpRequest() throws MalformedURLException {

        if (requestAuthority.tokenEndpointUrl() == null) {
            throw new MsalClientException("The endpoint URI is not specified",
                    AuthenticationErrorCode.INVALID_ENDPOINT_URI);
        }

        final OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
                HttpMethod.POST,
                requestAuthority.tokenEndpointUrl(),
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.serviceBundle);

        final Map<String, String> params = new HashMap<>(msalRequest.msalAuthorizationGrant().toParameters());
        if (msalRequest.application() instanceof AbstractClientApplicationBase
                && ((AbstractClientApplicationBase) msalRequest.application()).clientCapabilities() != null) {
            params.put("claims", ((AbstractClientApplicationBase) msalRequest.application()).clientCapabilities());
        }

        if (msalRequest.msalAuthorizationGrant.getClaims() != null) {
            String claimsRequest = msalRequest.msalAuthorizationGrant.getClaims().formatAsJSONString();
            if (params.get("claims") != null) {
                claimsRequest = JsonHelper.mergeJSONString(params.get("claims"), claimsRequest);
            }
            params.put("claims", claimsRequest);
        }

        // Client-originated claims (claimsFromClient) are forwarded on the wire as a standard OAuth
        // "claims" parameter. They are merged here, after the capabilities/server-claims merge above,
        // because that logic rebuilds the "claims" param and would otherwise clobber an earlier value.
        // This single point covers every flow whose parameters expose clientClaims(): the
        // confidential-client flows (client credentials, OBO, user-FIC) and the authorization-code
        // flow (web-app code redemption, on either a confidential or public client). Flows that do
        // not set client claims return null/blank here and are unaffected.
        String clientClaims = msalRequest.requestContext().apiParameters().clientClaims();
        if (!StringHelper.isBlank(clientClaims)) {
            if (params.get("claims") != null) {
                params.put("claims", JsonHelper.mergeJSONString(params.get("claims"), clientClaims));
            } else {
                params.put("claims", clientClaims);
            }
        }

        if(msalRequest.requestContext().apiParameters().extraQueryParameters() != null ){
            for(String key: msalRequest.requestContext().apiParameters().extraQueryParameters().keySet()){
                    if(params.containsKey(key)){
                       LOG.warn("A query parameter {} has been provided with values multiple times.", key);
                    }
                    params.put(key, msalRequest.requestContext().apiParameters().extraQueryParameters().get(key));
            }
        }

        oauthHttpRequest.setQuery(StringHelper.serializeQueryParameters(params));

        //Certain query parameters are required by Public and Confidential client applications, but not Managed Identity
        if (msalRequest.application() instanceof AbstractClientApplicationBase) {
            addQueryParameters(oauthHttpRequest);
        }
        return oauthHttpRequest;
    }

    OAuthHttpRequest createOauthHttpRequest(
            URL tokenEndpoint,
            SSLSocketFactory sslSocketFactory,
            Map<String, String> parameters) {
        if (tokenEndpoint == null) {
            throw new MsalClientException("The endpoint URI is not specified",
                    AuthenticationErrorCode.INVALID_ENDPOINT_URI);
        }

        OAuthHttpRequest request = new OAuthHttpRequest(
                HttpMethod.POST,
                tokenEndpoint,
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                serviceBundle)
                .sslSocketFactory(sslSocketFactory);

        Map<String, String> params = new HashMap<>(parameters);
        mergeClaimsAndCapabilities(params);
        request.setQuery(StringHelper.serializeQueryParameters(params));
        return request;
    }

    OAuthHttpRequest createOauthHttpRequest(
            URL tokenEndpoint,
            SSLContext sslContext,
            Map<String, String> parameters) {
        if (tokenEndpoint == null) {
            throw new MsalClientException("The endpoint URI is not specified",
                    AuthenticationErrorCode.INVALID_ENDPOINT_URI);
        }

        OAuthHttpRequest request = new OAuthHttpRequest(
                HttpMethod.POST,
                tokenEndpoint,
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                serviceBundle)
                .sslContext(sslContext);

        Map<String, String> params = new HashMap<>(parameters);
        mergeClaimsAndCapabilities(params);
        request.setQuery(StringHelper.serializeQueryParameters(params));
        return request;
    }

    private void mergeClaimsAndCapabilities(Map<String, String> params) {
        String claims = params.get("claims");
        if (msalRequest.application() instanceof AbstractClientApplicationBase
                && ((AbstractClientApplicationBase) msalRequest.application()).clientCapabilities() != null) {
            claims = mergeClaims(
                    claims,
                    ((AbstractClientApplicationBase) msalRequest.application()).clientCapabilities());
        } else if (msalRequest.application() instanceof ManagedIdentityApplication) {
            List<String> capabilities =
                    ((ManagedIdentityApplication) msalRequest.application()).getClientCapabilities();
            if (capabilities != null && !capabilities.isEmpty()) {
                claims = mergeClaims(
                        claims,
                        JsonHelper.formCapabilitiesJson(new HashSet<>(capabilities)));
            }
        }

        ClaimsRequest requestClaims = msalRequest.requestContext().apiParameters().claims();
        if (requestClaims != null) {
            claims = mergeClaims(claims, requestClaims.formatAsJSONString());
        }
        if (!StringHelper.isBlank(claims)) {
            params.put("claims", claims);
        }
    }

    private static String mergeClaims(String first, String second) {
        if (StringHelper.isBlank(first)) {
            return second;
        }
        if (StringHelper.isBlank(second)) {
            return first;
        }
        return JsonHelper.mergeJSONString(first, second);
    }

    private void addQueryParameters(OAuthHttpRequest oauthHttpRequest) {
        Map<String, String> queryParameters = StringHelper.parseQueryParameters(oauthHttpRequest.query);
        String clientID = msalRequest.application().clientId();
        queryParameters.put("client_id", clientID);

        // Add client authentication parameters if this is a confidential client
        if (msalRequest.application() instanceof ConfidentialClientApplication) {
            ConfidentialClientApplication application = (ConfidentialClientApplication) msalRequest.application();

            // Consolidated credential and tenant override handling
            addCredentialToRequest(queryParameters, application);
        }

        oauthHttpRequest.setQuery(StringHelper.serializeQueryParameters(queryParameters));
    }

    /**
     * Adds the appropriate authentication parameters to the request based on credential type.
     * Handles different credential types (secret, assertion, certificate) by adding the appropriate
     * parameters to the request.
     *
     * @param queryParameters The map of query parameters to add to
     * @param application The confidential client application
     */
    private void addCredentialToRequest(Map<String, String> queryParameters,
                                       ConfidentialClientApplication application) {
        IClientCredential credentialToUse = application.clientCredential;
        Authority authorityToUse = application.authenticationAuthority;

        // A ClientCredentialRequest may have parameters which override the credentials used to build the application.
        if (msalRequest instanceof ClientCredentialRequest) {
            ClientCredentialParameters parameters = ((ClientCredentialRequest) msalRequest).parameters;

            if (parameters.clientCredential() != null) {
                credentialToUse = parameters.clientCredential();
            }

            if (parameters.tenant() != null) {
                try {
                    authorityToUse = Authority.replaceTenant(authorityToUse, parameters.tenant());
                } catch (MalformedURLException e) {
                    LOG.warn("Could not create authority with tenant override: {}", e.getMessage());
                }
            }
        }

        // Quick return if no credential is provided
        if (credentialToUse == null) {
            return;
        }

        if (credentialToUse instanceof ClientSecret) {
            // For client secret, add client_secret parameter
            queryParameters.put("client_secret", ((ClientSecret) credentialToUse).clientSecret());
        } else if (credentialToUse instanceof ClientAssertion) {
            // For client assertion, add client_assertion and client_assertion_type parameters
            ClientAssertion clientAssertion = (ClientAssertion) credentialToUse;
            if (clientAssertion.isContextAware()) {
                // Build assertion context with client assertion FMI path if available
                String clientAssertionFmiPath = null;
                if (msalRequest instanceof ClientCredentialRequest) {
                    clientAssertionFmiPath = ((ClientCredentialRequest) msalRequest).parameters.fmiPath();
                }
                String tokenEndpoint = null;
                try {
                    tokenEndpoint = authorityToUse.tokenEndpointUrl() != null
                            ? authorityToUse.tokenEndpointUrl().toString() : null;
                } catch (MalformedURLException e) {
                    LOG.warn("Could not resolve token endpoint URL for assertion context: {}", e.getMessage());
                }
                AssertionRequestOptions options = new AssertionRequestOptions(
                        application.clientId(),
                        tokenEndpoint,
                        clientAssertionFmiPath);

                addJWTBearerAssertionParams(queryParameters, clientAssertion.assertion(options));
            } else {
                addJWTBearerAssertionParams(queryParameters, clientAssertion.assertion());
            }
        } else if (credentialToUse instanceof ClientCertificate) {
            // For client certificate, generate a new assertion and add it to the request
            ClientCertificate certificate = (ClientCertificate) credentialToUse;
            String assertion = certificate.getAssertion(
                authorityToUse,
                application.clientId(),
                application.sendX5c());
            addJWTBearerAssertionParams(queryParameters, assertion);
        }
    }

    /**
     * Adds the JWT bearer token assertion parameters to the request
     *
     * @param queryParameters The map of query parameters to add to
     * @param assertion The JWT assertion string
     */
    private void addJWTBearerAssertionParams(Map<String, String> queryParameters, String assertion) {
        queryParameters.put("client_assertion", assertion);
        queryParameters.put("client_assertion_type", ClientAssertion.ASSERTION_TYPE_JWT_BEARER);
    }

    private AuthenticationResult createAuthenticationResultFromOauthHttpResponse(HttpResponse oauthHttpResponse) {
        AuthenticationResult result;

        if (oauthHttpResponse.statusCode() == HttpStatus.HTTP_OK) {
            final TokenResponse response = TokenResponse.parseHttpResponse(oauthHttpResponse);

            AccountCacheEntity accountCacheEntity = null;
            if (!StringHelper.isNullOrBlank(response.idToken())) {
                IdToken idToken = JsonHelper.createIdTokenFromEncodedTokenString(response.idToken());

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
                    tokenType(response.tokenType()).
                    metadata(AuthenticationResultMetadata.builder()
                            .tokenSource(TokenSource.IDENTITY_PROVIDER)
                            .refreshOn(response.getRefreshIn() > 0 ? currTimestampSec + response.getRefreshIn() : 0)
                            .build()).
                    build();

        } else {
            // http codes indicating that STS did not log request
            if (oauthHttpResponse.statusCode() == HttpStatus.HTTP_TOO_MANY_REQUESTS || oauthHttpResponse.statusCode() >= HttpStatus.HTTP_INTERNAL_ERROR) {
                serviceBundle.getServerSideTelemetry().previousRequests.putAll(
                        serviceBundle.getServerSideTelemetry().previousRequestInProgress);
            }

            throw MsalServiceExceptionFactory.fromHttpResponse(oauthHttpResponse);
        }
        return result;
    }

    Logger getLog() {
        return LOG;
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