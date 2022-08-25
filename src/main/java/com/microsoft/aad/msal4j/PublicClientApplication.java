// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.LoggerFactory;

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
    @Accessors(fluent = true)
    @Getter
    private Boolean allowBroker; //TODO: javadocs explaining what this enables

    //TODO: decide whether to allow devs to add a custom path, or just use some default
    private String msalruntimePath;

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(UserNamePasswordParameters parameters) {

        validateNotNull("parameters", parameters);

        RequestContext context = new RequestContext(
                this,
                PublicApi.ACQUIRE_TOKEN_BY_USERNAME_PASSWORD,
                parameters,
                UserIdentifier.fromUpn(parameters.username()));

        UserNamePasswordRequest userNamePasswordRequest =
                new UserNamePasswordRequest(parameters,
                        this,
                        context);

        return this.executeRequest(userNamePasswordRequest);
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

        CompletableFuture<IAuthenticationResult> future = executeRequest(interactiveRequest);
        futureReference.set(future);
        return future;
    }

    private PublicClientApplication(Builder builder) {
        super(builder);
        validateNotBlank("clientId", clientId());
        log = LoggerFactory.getLogger(PublicClientApplication.class);
        this.clientAuthentication = new ClientAuthenticationPost(ClientAuthenticationMethod.NONE,
                new ClientID(clientId()));
        allowBroker = builder.allowBroker;
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

        private boolean allowBroker = false;

        private Builder(String clientId) {
            super(clientId);
        }

        @Override
        public PublicClientApplication build() {

            return new PublicClientApplication(this);
        }

        //TODO: javadocs
        public Builder allowBroker(Boolean val) {
            this.allowBroker = val;

            return self();
        }

        @Override
        protected Builder self() {
            return this;
        }
    }
}