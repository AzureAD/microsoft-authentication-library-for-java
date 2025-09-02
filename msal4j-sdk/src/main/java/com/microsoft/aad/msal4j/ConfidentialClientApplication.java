// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.slf4j.LoggerFactory;

import java.util.Base64;
import java.util.Date;
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

    private ClientCertificate clientCertificate;
    private String assertion;
    private IClientCredential clientCredential;
    String secret;

    /** AppTokenProvider creates a Credential from a function that provides access tokens. The function
     must be concurrency safe. This is intended only to allow the Azure SDK to cache MSI tokens. It isn't
     useful to applications in general because the token provider must implement all authentication logic. */
    public Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

    private boolean sendX5c;

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

    private ConfidentialClientApplication(Builder builder) {
        super(builder);
        sendX5c = builder.sendX5c;
        appTokenProvider = builder.appTokenProvider;

        log = LoggerFactory.getLogger(ConfidentialClientApplication.class);

        initClientAuthentication(builder.clientCredential);

        this.tenant = this.authenticationAuthority.tenant;
    }

    private void initClientAuthentication(IClientCredential clientCredential) {
        validateNotNull("clientCredential", clientCredential);

        this.clientCredential = clientCredential;

        if (clientCredential instanceof ClientSecret) {
            this.secret = ((ClientSecret) clientCredential).clientSecret();
        } else if (clientCredential instanceof ClientCertificate) {
            this.clientCertificate = (ClientCertificate) clientCredential;
            this.assertion = getAssertionString(clientCredential);
        } else if (clientCredential instanceof ClientAssertion) {
            this.assertion = getAssertionString(clientCredential);
        } else {
            throw new IllegalArgumentException("Unsupported client credential");
        }
    }

    /**
     * Generates a JWT-formatted assertion string based on the provided client credential. Returns null in cases where
     * the request for that credential type would not use a JWT assertion (e.g. client secret).
     *
     * @param clientCredential  The client credential to use for token acquisition.
     * @return JWT-formatted assertion string
     */
    String getAssertionString(IClientCredential clientCredential) {
        if (clientCredential instanceof ClientCertificate) {
            // Check if the current assertion is null or has expired, and if so create a new one
            if (this.assertion == null || hasJwtExpired(this.assertion)) {
                boolean useSha1 = Authority.detectAuthorityType(this.authenticationAuthority.canonicalAuthorityUrl()) == AuthorityType.ADFS;

                this.assertion = JwtHelper.buildJwt(
                        clientId(),
                        clientCertificate,
                        this.authenticationAuthority.selfSignedJwtAudience(),
                        sendX5c,
                        useSha1).assertion();
            }
            return this.assertion;
        } else if (clientCredential instanceof ClientAssertion) {
            return ((ClientAssertion) clientCredential).assertion();
        } else if (clientCredential instanceof ClientSecret) {
            return null;
        } else {
            throw new IllegalArgumentException("Unsupported client credential");
        }
    }

    //Overload for the common case where the application's default credential was not overridden in the request.
    String getAssertionString() {
        return this.getAssertionString(this.clientCredential);
    }

    /**
     * Checks if the JWT-formatted assertion has expired by parsing the "exp" claim.
     *
     * @param jwt JWT string
     * @return true if the JWT has expired. Otherwise false
     */
    boolean hasJwtExpired(String jwt) {
        final Date currentDateTime = new Date(System.currentTimeMillis());
        Base64.Decoder decoder = Base64.getUrlDecoder();

        String payload = new String(decoder.decode(jwt.split("\\.")[1]));

        final Date expirationTime = (Date) JsonHelper.parseJsonToMap(payload).get("exp");

        return expirationTime.before(currentDateTime);
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

    public static class Builder extends AbstractClientApplicationBase.Builder<Builder> {

        private IClientCredential clientCredential;

        private boolean sendX5c = true;

        private Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> appTokenProvider;

        private Builder(String clientId, IClientCredential clientCredential) {
            super(clientId);
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