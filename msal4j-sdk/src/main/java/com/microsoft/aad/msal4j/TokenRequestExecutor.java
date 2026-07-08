// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

class TokenRequestExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(TokenRequestExecutor.class);

    final Authority requestAuthority;
    final String tenant;
    private final MsalRequest msalRequest;
    private final ServiceBundle serviceBundle;

    // For mTLS Proof-of-Possession requests, the certificate presented on the TLS handshake. Resolved once
    // when building the request and reused to describe the binding certificate on the result.
    private IClientCertificate resolvedBindingCertificate;

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

    OAuthHttpRequest createOauthHttpRequest() throws MalformedURLException {

        if (requestAuthority.tokenEndpointUrl() == null) {
            throw new MsalClientException("The endpoint URI is not specified",
                    AuthenticationErrorCode.INVALID_ENDPOINT_URI);
        }

        URL tokenEndpointUrl = requestAuthority.tokenEndpointUrl();
        IHttpClient mtlsHttpClient = null;

        if (isMtlsProofOfPossession()) {
            ConfidentialClientApplication application = (ConfidentialClientApplication) msalRequest.application();
            this.resolvedBindingCertificate = resolveBindingCertificate(application);
            tokenEndpointUrl = MtlsEndpointHelper.deriveMtlsTokenEndpoint(tokenEndpointUrl);
            SSLSocketFactory mtlsSocketFactory =
                    MtlsClientCertificateHelper.createMtlsSocketFactory(resolvedBindingCertificate);
            mtlsHttpClient = new DefaultHttpClient(
                    application.proxy(),
                    mtlsSocketFactory,
                    application.connectTimeoutForDefaultHttpClient(),
                    application.readTimeoutForDefaultHttpClient());
            LOG.debug("mTLS Proof-of-Possession requested; using mTLS token endpoint: {}", tokenEndpointUrl);
        }

        final OAuthHttpRequest oauthHttpRequest = new OAuthHttpRequest(
                HttpMethod.POST,
                tokenEndpointUrl,
                msalRequest.headers().getReadonlyHeaderMap(),
                msalRequest.requestContext(),
                this.serviceBundle);

        if (mtlsHttpClient != null) {
            oauthHttpRequest.setMtlsHttpClient(mtlsHttpClient);
        }

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

        boolean mtlsPoP = isMtlsProofOfPossession();

        if (credentialToUse instanceof ClientSecret) {
            // For client secret, add client_secret parameter
            queryParameters.put("client_secret", ((ClientSecret) credentialToUse).clientSecret());
        } else if (credentialToUse instanceof ClientAssertion) {
            // For client assertion, add client_assertion and client_assertion_type parameters
            ClientAssertion clientAssertion = (ClientAssertion) credentialToUse;
            String assertion;
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
                        clientAssertionFmiPath,
                        mtlsPoP);

                assertion = clientAssertion.assertion(options);
            } else {
                assertion = clientAssertion.assertion();
            }

            // For mTLS PoP (FIC Leg 2), the assertion is authenticated with the jwt-pop assertion type and
            // the binding certificate is presented on the TLS handshake.
            addJWTAssertionParams(queryParameters, assertion,
                    mtlsPoP ? ClientAssertion.ASSERTION_TYPE_JWT_POP : ClientAssertion.ASSERTION_TYPE_JWT_BEARER);
        } else if (credentialToUse instanceof ClientCertificate) {
            if (mtlsPoP) {
                // For mTLS PoP (direct SN/I cert / FIC Leg 1), the certificate is presented as the client
                // TLS certificate and authenticates the client; no client_assertion is sent (ESTS resolves
                // SN/I trust from the TLS-presented certificate and binds the token via x5t#S256/cnf).
                return;
            }
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
        addJWTAssertionParams(queryParameters, assertion, ClientAssertion.ASSERTION_TYPE_JWT_BEARER);
    }

    /**
     * Adds the JWT assertion parameters to the request with the given client_assertion_type.
     *
     * @param queryParameters The map of query parameters to add to
     * @param assertion The JWT assertion string
     * @param assertionType The client_assertion_type value (jwt-bearer for Bearer, jwt-pop for mTLS PoP)
     */
    private void addJWTAssertionParams(Map<String, String> queryParameters, String assertion, String assertionType) {
        queryParameters.put("client_assertion", assertion);
        queryParameters.put("client_assertion_type", assertionType);
    }

    /**
     * @return true if this request opted into mTLS Proof-of-Possession via
     * {@link ClientCredentialParameters.ClientCredentialParametersBuilder#mtlsProofOfPossession()}.
     */
    private boolean isMtlsProofOfPossession() {
        return msalRequest instanceof ClientCredentialRequest
                && ((ClientCredentialRequest) msalRequest).parameters.mtlsProofOfPossession();
    }

    /**
     * Resolves the certificate to present as the client TLS certificate for an mTLS PoP request: the
     * request/app authentication credential if it is a certificate (direct SN/I cert or FIC Leg 1),
     * otherwise the configured {@code mtlsBindingCertificate} (FIC Leg 2, assertion-authenticated).
     */
    private IClientCertificate resolveBindingCertificate(ConfidentialClientApplication application) {
        ClientCredentialParameters parameters = msalRequest instanceof ClientCredentialRequest
                ? ((ClientCredentialRequest) msalRequest).parameters : null;
        return MtlsClientCertificateHelper.resolveBindingCertificate(application, parameters);
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

            // The token type is taken from the identity provider's response, never assumed from the request.
            TokenType tokenType = TokenType.fromString(response.tokenType());
            BindingCertificate bindingCertificate = null;
            if (isMtlsProofOfPossession()) {
                // Fail closed: an mTLS Proof-of-Possession request MUST come back as an mtls_pop
                // (certificate-bound) token. If ESTS returns a different token_type (for example a Bearer
                // downgrade), the access token is not bound to the certificate; surfacing it as MTLS_POP
                // would mask the downgrade, so reject it instead.
                if (tokenType != TokenType.MTLS_POP) {
                    throw new MsalClientException(
                            String.format("An mTLS Proof-of-Possession token was requested, but the identity " +
                                    "provider returned token_type '%s' instead of 'mtls_pop'. The access token is " +
                                    "not certificate-bound.", response.tokenType()),
                            AuthenticationErrorCode.TOKEN_TYPE_MISMATCH);
                }
                // Surface the binding certificate only after the token type has been validated.
                bindingCertificate = MtlsClientCertificateHelper.buildBindingCertificate(resolvedBindingCertificate);
            }

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
                            .tokenType(tokenType)
                            .bindingCertificate(bindingCertificate)
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