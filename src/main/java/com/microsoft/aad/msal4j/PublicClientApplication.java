// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

/**
 * Class to be used to acquire tokens for public client applications (Desktop, Mobile).
 */
public class PublicClientApplication extends ClientApplicationBase implements IPublicClientApplication {

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(UserNamePasswordParameters parameters) {

        validateNotNull("parameters", parameters);

        UserNamePasswordRequest userNamePasswordRequest =
                new UserNamePasswordRequest(parameters,
                        this,
                        createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_USERNAME_PASSWORD));

        return this.executeRequest(userNamePasswordRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(IntegratedWindowsAuthenticationParameters parameters) {

        validateNotNull("parameters", parameters);

        IntegratedWindowsAuthenticationRequest integratedWindowsAuthenticationRequest =
                new IntegratedWindowsAuthenticationRequest(
                        parameters,
                        this,
                        createRequestContext(
                                PublicApi.ACQUIRE_TOKEN_BY_INTEGRATED_WINDOWS_AUTH));

        return this.executeRequest(integratedWindowsAuthenticationRequest);
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(DeviceCodeFlowParameters parameters) {

        if (!AuthorityType.AAD.equals(authenticationAuthority.authorityType())) {
            throw new IllegalArgumentException(
                    "Invalid authority type. Device Flow is only supported by AAD authority");
        }

        validateNotNull("parameters", parameters);

        AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference =
                new AtomicReference<>();

        DeviceCodeFlowRequest deviceCodeRequest = new DeviceCodeFlowRequest(
                parameters,
                futureReference,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW));

        CompletableFuture<IAuthenticationResult> future = executeRequest(deviceCodeRequest);
        futureReference.set(future);
        return future;
    }

    @Override
    public CompletableFuture<IAuthenticationResult> acquireToken(InteractiveRequestParameters parameters){

        validateNotNull("parameters", parameters);

        AtomicReference<CompletableFuture<IAuthenticationResult>> futureReference = new AtomicReference<>();

        InteractiveRequest interactiveRequest = new InteractiveRequest(
                parameters,
                futureReference,
                this,
                createRequestContext(PublicApi.ACQUIRE_TOKEN_BY_AUTHORIZATION_CODE));

        CompletableFuture<IAuthenticationResult> future = executeRequest(interactiveRequest);
        futureReference.set(future);
        return future;
    }

    private PublicClientApplication(Builder builder) {
        super(builder);

        log = LoggerFactory.getLogger(PublicClientApplication.class);

        initClientAuthentication(clientId());
    }

    private void initClientAuthentication(String clientId) {
        validateNotBlank("clientId", clientId);

        clientAuthentication = new ClientAuthenticationPost(ClientAuthenticationMethod.NONE,
                new ClientID(clientId));
    }

    /**
     * @param clientId Client ID (Application ID) of the application as registered
     *                 in the application registration portal (portal.azure.com)
     * @return instance of Builder of PublicClientApplication
     */
    public static Builder builder(String clientId) {

        return new Builder(clientId);
    }

    public static class Builder extends ClientApplicationBase.Builder<Builder> {

        private Builder(String clientId) {
            super(clientId);
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