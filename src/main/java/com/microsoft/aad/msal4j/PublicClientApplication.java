// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotBlank;
import static com.microsoft.aad.msal4j.ParameterValidationUtils.validateNotNull;

public class PublicClientApplication extends ClientApplicationBase {

    /**
     * Acquires a security token using a username/password flow.
     *
     * @param parameters#scopes   scopes of the access request
     * @param parameters#username Username of the managed or federated user.
     * @param parameters#password Password of the managed or federated user.
     *                            If null, integrated authentication will be used.
     * @return A {@link CompletableFuture} object representing the
     * {@link AuthenticationResult} of the call. It contains Access
     * Token, Refresh Token and the Access Token's expiration time.
     */
    public CompletableFuture<AuthenticationResult> acquireToken(UserNamePasswordParameters parameters) {

        validateNotNull("parameters", parameters);

        UserNamePasswordRequest userNamePasswordRequest =
                new UserNamePasswordRequest(parameters,
                        this,
                        createRequestContext(AcquireTokenPublicApi.ACQUIRE_TOKEN_BY_USERNAME_PASSWORD));

        return this.executeRequest(userNamePasswordRequest);
    }

    /**
     * Acquires a security token using Windows integrated authentication flow.
     *
     * @param parameters#scopes   scopes of the access request
     * @param parameters#username Username of the managed or federated user.
     * @return A {@link CompletableFuture} object representing the
     * {@link AuthenticationResult} of the call. It contains Access
     * Token, Refresh Token and the Access Token's expiration time.
     */
    public CompletableFuture<AuthenticationResult> acquireToken(IntegratedWindowsAuthenticationParameters parameters) {

        validateNotNull("parameters", parameters);

        IntegratedWindowsAuthenticationRequest integratedWindowsAuthenticationRequest =
                new IntegratedWindowsAuthenticationRequest(
                        parameters,
                        this,
                        createRequestContext(
                                AcquireTokenPublicApi.ACQUIRE_TOKEN_BY_INTEGRATED_WINDOWS_AUTH));

        return this.executeRequest(integratedWindowsAuthenticationRequest);
    }

    /**
     * Acquires security token from the authority using an device code flow.
     * <p>
     * Flow is designed for devices that do not have access to a browser or have input constraints.
     * The authorization server issues DeviceCode object with verification code, an end-user code
     * and the end-user verification URI. DeviceCode is provided through deviceCodeConsumer callback.
     * End-user should be instructed to use another device to connect to the authorization server to approve the access request.
     * <p>
     * Since the client cannot receive incoming requests, it polls the authorization server repeatedly
     * until the end-user completes the approval process.
     *
     * @param parameters#scopes             scopes of the access request
     * @param parameters#deviceCodeConsumer
     * @return A {@link CompletableFuture} object representing the {@link AuthenticationResult} of the call.
     * It contains AccessTokenCacheEntity, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException thrown if authorization is pending or another error occurred.
     *                                 If the errorCode of the exception is AuthenticationErrorCode.AUTHORIZATION_PENDING,
     *                                 the call needs to be retried until the AccessToken is returned.
     *                                 DeviceCode.interval - The minimum amount of time in seconds that the client
     *                                 SHOULD wait between polling requests to the token endpoint
     */
    public CompletableFuture<AuthenticationResult> acquireToken(DeviceCodeFlowParameters parameters) {

        if (!AuthorityType.AAD.equals(authenticationAuthority.getAuthorityType())) {
            throw new IllegalArgumentException(
                    "Invalid authority type. Device Flow is only supported by AAD authority");
        }

        validateNotNull("parameters", parameters);

        AtomicReference<CompletableFuture<AuthenticationResult>> futureReference =
                new AtomicReference<>();

        DeviceCodeFlowRequest deviceCodeRequest = new DeviceCodeFlowRequest(
                parameters,
                futureReference,
                this,
                createRequestContext(AcquireTokenPublicApi.ACQUIRE_TOKEN_BY_DEVICE_CODE_FLOW));

        CompletableFuture<AuthenticationResult> future = executeRequest(deviceCodeRequest);
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
     * Returns instance of Builder of PublicClientApplication
     *
     * @param clientId Client ID (Application ID) of the application as registered
     *                 in the application registration portal (portal.azure.com)
     */
    public static Builder builder(String clientId) {

        return new Builder(clientId);
    }

    public static class Builder extends ClientApplicationBase.Builder<Builder> {
        /**
         * Constructor to create instance of Builder of PublicClientApplication
         *
         * @param clientId Client ID (Application ID) of the application as registered
         *                 in the application registration portal (portal.azure.com)
         */
        Builder(String clientId) {
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