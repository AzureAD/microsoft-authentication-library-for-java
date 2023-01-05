// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Class to be used to acquire tokens for public client applications (Desktop, Mobile).
 * For details see {@link IPublicClientApplication}
 * <p>
 * Conditionally thread-safe
 */
public class PublicClientApplication extends AbstractClientApplicationBase implements IPublicClientApplication {

    private final ClientAuthenticationPost clientAuthentication;
    private IBroker broker;

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(UserNamePasswordParameters parameters) {

        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_USERNAME_PASSWORD,
                parameters,
                UserIdentifier.fromUpn(parameters.username()));

        CompletableFuture<IAuthenticationResult> future;

        if (this.broker != null) {
            future = broker.acquireToken(this, parameters);
        } else {
            UserNamePasswordRequest userNamePasswordRequest =
                    new UserNamePasswordRequest(parameters,
                            this,
                            context);

            future = this.executeRequest(userNamePasswordRequest);
        }

        return future;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(IntegratedWindowsAuthenticationParameters parameters) {

        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_INTEGRATED_WINDOWS_AUTH,
                parameters,
                UserIdentifier.fromUpn(parameters.username()));

        IntegratedWindowsAuthenticationRequest integratedWindowsAuthenticationRequest =
                new IntegratedWindowsAuthenticationRequest(
                        parameters,
                        this,
                        context);

        return this.executeRequest(integratedWindowsAuthenticationRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(DeviceCodeFlowParameters parameters) {

        if (!(AuthorityType.AAD.equals(authenticationAuthority.authorityType()) ||
                AuthorityType.ADFS.equals(authenticationAuthority.authorityType()))) {
            throw new IllegalArgumentException(
                    "Invalid authority type. Device Flow is only supported by AAD and ADFS authorities");
        }

        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW,
                parameters);

        AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference =
                new AtomicReference<>();

        DeviceCodeFlowRequest deviceCodeRequest = new DeviceCodeFlowRequest(
                parameters,
                futureReference,
                this,
                context);

        CompletableFuture<IAuthenticationResult> future = executeRequest(deviceCodeRequest);
        futureReference.set(future);
        return future;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(InteractiveRequestParameters parameters) {

        validateNotNull("parameters", parameters);

        AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference = new AtomicReference<>();

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_INTERACTIVE,
                parameters,
                UserIdentifier.fromUpn(parameters.loginHint()));

        InteractiveRequest interactiveRequest = new InteractiveRequest(
                parameters,
                futureReference,
                this,
                context);

        CompletableFuture<IAuthenticationResult> future;

        if (this.broker != null) {
            future = broker.acquireToken(this, parameters);
        } else {
            future = executeRequest(interactiveRequest);
        }

        futureReference.set(future);

        return future;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters) throws MalformedURLException {
        CompletableFuture<IAuthenticationResult> future;

        if (this.broker != null) {
            future = broker.acquireToken(this, parameters);
        } else {
            future = super.acquireTokenSilently(parameters);
        }

        return future;
    }

    @Override
    public CompletableFuture<Void> removeAccount(IAccount account) {
        if (this.broker != null) {
            broker.removeAccount(this, account);
        }

        return super.removeAccount(account);
    }

    private PublicClientApplication(Builder builder) {
        super(builder);
        validateNotBlank("clientId", clientId());
        log = LoggerFactory.getLogger(PublicClientApplication.class);
        this.clientAuthentication = new ClientAuthenticationPost(ClientAuthenticationMethod.NONE,
                new ClientID(clientId()));
        this.broker = builder.broker;
    }

    @Override
    protected ClientAuthentication clientAuthentication() {
        return clientAuthentication;
    }

    /**
     * @param clientId Client ID (Application ID) of the application as registered
     *                 in the application registration portal (portal.azure.com)
     * @return instance of Builder of PublicClientApplication
     */
    public static Builder builder(String clientId) {

        return new Builder(clientId);
    }

    public static class Builder extends AbstractClientApplicationBase.Builder<Builder> {

        private Builder(String clientId) {
            super(clientId);
        }

        private IBroker broker = null;

        /**
         * Implementation of IBroker that will be used to retrieve tokens
         * <p>
         * Setting this will cause MSAL Java to use the given broker implementation to retrieve tokens from a broker (such as WAM/MSALRuntime) in flows that support it
         */
        public PublicClientApplication.Builder broker(IBroker val) {
            this.broker = val;

            return self();
        }

        @Override
        public PublicClientApplication build() {

            return new PublicClientApplication(this);
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}