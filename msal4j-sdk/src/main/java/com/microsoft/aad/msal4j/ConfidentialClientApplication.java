// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Class to be used to acquire tokens for confidential client applications (Web Apps, Web APIs,
 * and daemon applications).
 * For details see {@link IConfidentialClientApplication}
 * <p>
 * Conditionally thread-safe
 */
public class ConfidentialClientApplication extends AbstractClientApplicationBase implements IConfidentialClientApplication {

    IClientCredential clientCredential;
    private boolean sendX5c;
    private boolean sendCertificateOverMtls;

    /** AppTokenProvider creates a Credential from a function that provides access tokens. The function
     must be concurrency safe. This is intended only to allow the Azure SDK to cache MSI tokens. It isn't
     useful to applications in general because the token provider must implement all authentication logic. */
    public Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters) {
        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_FOR_CLIENT,
                parameters);

        ClientCredentialRequest clientCredentialRequest =
                new ClientCredentialRequest(
                        parameters,
                        this,
                        context,
                        appTokenProvider);

        return this.executeRequest(clientCredentialRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters) {
        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_ON_BEHALF_OF,
                parameters);

        OnBehalfOfRequest oboRequest = new OnBehalfOfRequest(
                parameters,
                this,
                context);

        return this.executeRequest(oboRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(UserFederatedIdentityCredentialParameters parameters) {
        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_USER_FEDERATED_IDENTITY_CREDENTIAL,
                parameters);

        UserFederatedIdentityCredentialRequest userFicRequest =
                new UserFederatedIdentityCredentialRequest(
                        parameters,
                        this,
                        context);

        return this.executeRequest(userFicRequest);
    }

    private ConfidentialClientApplication(Builder builder) {
        super(builder);
        sendX5c = builder.sendX5c;
        sendCertificateOverMtls = builder.sendCertificateOverMtls;
        appTokenProvider = builder.appTokenProvider;

        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);

        this.clientCredential = builder.clientCredential;

        this.tenant = this.authenticationAuthority.tenant;
    }

    /**
     * Creates instance of Builder of ConfidentialClientApplication
     *
     * @param clientId         Client ID (Application ID) of the application as registered
     *                         in the application registration portal (portal.azure.com)
     * @param clientCredential The client credential to use for token acquisition.
     * @return instance of Builder of ConfidentialClientApplication
     */
    public static Builder builder(String clientId, IClientCredential clientCredential) {

        return new Builder(clientId, clientCredential);
    }

    public boolean sendX5c() {
        return this.sendX5c;
    }

    @Override
    public boolean sendCertificateOverMtls() {
        return this.sendCertificateOverMtls;
    }

    public static class Builder extends AbstractClientApplicationBase.Builder<Builder> {

        private IClientCredential clientCredential;

        private boolean sendX5c = true;

        private boolean sendCertificateOverMtls = false;

        private Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

        private Builder(String clientId, IClientCredential clientCredential) {
            super(clientId);

            validateNotNull("clientCredential", clientCredential);

            this.clientCredential = clientCredential;
        }

        /**
         * Specifies if the x5c claim (public key of the certificate) should be sent to the STS.
         * Default value is true
         *
         * @param val true if the x5c should be sent. Otherwise false
         * @return instance of the Builder on which method was called
         */
        public ConfidentialClientApplication.Builder sendX5c(boolean val) {
            this.sendX5c = val;

            return self();
        }

        /**
         * Specifies whether the application's certificate credential is presented as the client certificate
         * on the mutual-TLS (mTLS) handshake to the token endpoint. When enabled, requests are routed to the
         * mTLS token endpoint ({@code mtlsauth.*}) and the identity provider returns a plain Bearer access
         * token (the token is NOT bound to the certificate).
         * <p>
         * This is distinct from per-request mTLS Proof-of-Possession
         * ({@link ClientCredentialParameters.ClientCredentialParametersBuilder#mtlsProofOfPossession()}),
         * which binds the token to the certificate ({@code token_type=mtls_pop}); a per-request mtls_pop
         * opt-in always takes precedence over this app-level flag. The flag is honored by every confidential
         * flow (client credentials, on-behalf-of, refresh token, authorization code).
         * <p>
         * Default value is {@code false}. When enabled, the application MUST be configured with a certificate
         * credential ({@link IClientCertificate}); otherwise {@link #build()} throws a
         * {@link MsalClientException} with error code
         * {@link AuthenticationErrorCode#CERTIFICATE_REQUIRED_FOR_MTLS}.
         *
         * @param val {@code true} to present the certificate over mTLS and receive a Bearer token
         * @return instance of the Builder on which method was called
         */
        public ConfidentialClientApplication.Builder sendCertificateOverMtls(boolean val) {
            this.sendCertificateOverMtls = val;

            return self();
        }

        /// <summary>
        /// Allows setting a callback which returns an access token, based on the passed-in parameters.
        /// MSAL will pass in its authentication parameters to the callback and it is expected that the callback
        /// will construct a <see cref="TokenProviderResult"/> and return it to MSAL.
        /// MSAL will cache the token response the same way it does for other authentication results.
        /// Note: This is only for client credential flows.
        /// </summary>
        /// <param name="appTokenProvider">Authentication callback which returns an access token.</param>
        /// <returns>The builder to chain the .With methods</returns>
        public ConfidentialClientApplication.Builder appTokenProvider(Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider){
            if(appTokenProvider!=null){
                this.appTokenProvider = appTokenProvider;
                return self();
            }

            throw new NullPointerException("appTokenProvider is null") ;
        }

        @Override
        public ConfidentialClientApplication build() {
            if (sendCertificateOverMtls && !(clientCredential instanceof IClientCertificate)) {
                throw new MsalClientException(
                        "sendCertificateOverMtls(true) requires a certificate credential (IClientCertificate) " +
                                "so it can be presented as the client certificate on the mTLS handshake. Configure " +
                                "the application with a certificate credential or disable sendCertificateOverMtls.",
                        AuthenticationErrorCode.CERTIFICATE_REQUIRED_FOR_MTLS);
            }

            return new ConfidentialClientApplication(this);
        }

        @Override
        protected ConfidentialClientApplication.Builder self() {
            return this;
        }
    }
}