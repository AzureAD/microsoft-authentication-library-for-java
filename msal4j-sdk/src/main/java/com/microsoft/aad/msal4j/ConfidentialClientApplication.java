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
    private IClientCertificate mtlsBindingCertificate;

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
        appTokenProvider = builder.appTokenProvider;

        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);

        this.clientCredential = builder.clientCredential;
        this.mtlsBindingCertificate = builder.mtlsBindingCertificate;

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

    /**
     * @return the certificate used as the client TLS certificate for mTLS Proof-of-Possession requests
     * when the application's authentication credential is not itself a certificate (e.g. FIC Leg 2, where
     * authentication is a federated assertion), or null if not configured.
     */
    public IClientCertificate mtlsBindingCertificate() {
        return this.mtlsBindingCertificate;
    }

    public static class Builder extends AbstractClientApplicationBase.Builder<Builder> {

        private IClientCredential clientCredential;

        private boolean sendX5c = true;

        private IClientCertificate mtlsBindingCertificate;

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
         * Configures a certificate to present as the client TLS certificate in the mutual-TLS handshake
         * for mTLS Proof-of-Possession requests (see
         * {@link ClientCredentialParameters.ClientCredentialParametersBuilder#mtlsProofOfPossession()}).
         * <p>
         * This is required only when the application authenticates with a credential that is <b>not</b>
         * itself a certificate — for example, FIC Leg 2, where the application authenticates with a
         * federated assertion ({@link ClientCredentialFactory#createFromClientAssertion(String)}) but must
         * still bind the resulting token to a certificate. When the application's authentication credential
         * is already an {@link IClientCertificate} (direct SN/I cert or FIC Leg 1), that same certificate is
         * used as the binding certificate and this option is unnecessary.
         * <p>
         * Only the certificate's public material is ever surfaced on the result (see
         * {@link AuthenticationResultMetadata#bindingCertificate()}); the private key is never exposed.
         *
         * @param val the binding certificate
         * @return instance of the Builder on which method was called
         */
        public ConfidentialClientApplication.Builder mtlsBindingCertificate(IClientCertificate val) {
            validateNotNull("mtlsBindingCertificate", val);
            this.mtlsBindingCertificate = val;

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

            return new ConfidentialClientApplication(this);
        }

        @Override
        protected ConfidentialClientApplication.Builder self() {
            return this;
        }
    }
}