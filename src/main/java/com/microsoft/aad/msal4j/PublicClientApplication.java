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

import com.nimbusds.oauth2.sdk.ResourceOwnerPasswordCredentialsGrant;
import com.nimbusds.oauth2.sdk.auth.ClientAuthenticationMethod;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.id.ClientID;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class PublicClientApplication extends ClientApplicationBase {

    private PublicClientApplication(Builder builder){
        super(builder);

        log = LoggerFactory.getLogger(PublicClientApplication.class);

        initClientAuthentication(clientId);
    }

    private void initClientAuthentication(String clientId){
        validateNotBlank("clientId", clientId);

        clientAuthentication = new ClientAuthenticationPost(ClientAuthenticationMethod.NONE,
                new ClientID(clientId));
    }

    /**
     * Acquires a security token using a username/password flow.
     *
     * @param scopes scopes of the access request
     * @param username
     *            Username of the managed or federated user.
     * @param password
     *            Password of the managed or federated user.
     *            If null, integrated authentication will be used.
     * @return A {@link CompletableFuture} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    public CompletableFuture<AuthenticationResult> acquireTokenByUsernamePassword(Set<String> scopes,
                                                                                  String username, String password) {
        validateNotEmpty("scopes", scopes);
        validateNotBlank("username", username);

        return this.acquireToken(new MsalOAuthAuthorizationGrant(
                new ResourceOwnerPasswordCredentialsGrant
                        (username, new Secret(password)), scopes), clientAuthentication);
    }

    /**
     * Acquires a security token using Windows integrated authentication flow.
     *
     * @param scopes scopes of the access request
     * @param username
     *            Username of the managed or federated user.
     * @return A {@link CompletableFuture} object representing the
     *         {@link AuthenticationResult} of the call. It contains Access
     *         Token, Refresh Token and the Access Token's expiration time.
     */
    public CompletableFuture<AuthenticationResult> acquireTokenByIntegratedWindowsAuth(Set<String> scopes, String username) {
        validateNotEmpty("scopes", scopes);
        validateNotBlank("username", username);

        return this.acquireToken
                (new MsalIntegratedAuthorizationGrant(username, scopes), clientAuthentication);
    }

    /**
     * Acquires security token from the authority using an device code flow.
     *
     * Flow is designed for devices that do not have access to a browser or have input constraints.
     * The authorization server issues DeviceCode object with verification code, an end-user code
     * and the end-user verification URI. DeviceCode is provided through deviceCodeConsumer callback.
     * End-user should be instructed to use another device to connect to the authorization server to approve the access request.
     *
     * Since the client cannot receive incoming requests, it polls the authorization server repeatedly
     * until the end-user completes the approval process.
     *
     * @param scopes scopes of the access request
     * @param deviceCodeConsumer
     * @return A {@link CompletableFuture} object representing the {@link AuthenticationResult} of the call.
     * It contains AccessToken, Refresh Token and the Access Token's expiration time.
     * @throws AuthenticationException thrown if authorization is pending or another error occurred.
     *                                 If the errorCode of the exception is AdalErrorCode.AUTHORIZATION_PENDING,
     *                                 the call needs to be retried until the AccessToken is returned.
     *                                 DeviceCode.interval - The minimum amount of time in seconds that the client
     *                                 SHOULD wait between polling requests to the token endpoint
     */
    public CompletableFuture<AuthenticationResult> acquireTokenDeviceCodeFlow(Set<String> scopes,
                                                                              Consumer<DeviceCode> deviceCodeConsumer)
    {
        validateDeviceCodeRequestInput(scopes);

        AtomicReference<CompletableFuture<AuthenticationResult>> futureReference = new AtomicReference<>();

        AcquireTokenDeviceCodeFlowSupplier supplier =
                new AcquireTokenDeviceCodeFlowSupplier
                        (this, clientAuthentication, scopes, deviceCodeConsumer, futureReference);

        CompletableFuture<AuthenticationResult> future =
                executorService != null ? CompletableFuture.supplyAsync(supplier, executorService)
                        : CompletableFuture.supplyAsync(supplier);
        futureReference.set(future);

        return future;
    }

    private void validateDeviceCodeRequestInput(Set<String> scopes) {
        validateNotEmpty("scopes", scopes);

        if (AuthorityType.ADFS.equals(authenticationAuthority.getAuthorityType())){
            throw new IllegalArgumentException(
                    "Invalid authority type. Device Flow is not supported by ADFS authority");
        }
    }

    public static class Builder extends ClientApplicationBase.Builder<Builder>{
        /**
         * Constructor to create instance of Builder of PublicClientApplication
         * @param clientId Client ID (Application ID) of the application as registered
         *                 in the application registration portal (portal.azure.com)
         */
        public Builder(String clientId){
            super(clientId);
        }

        @Override public PublicClientApplication build() {

            return new PublicClientApplication(this);
        }

        @Override protected Builder self()
        {
            return this;
        }
    }
}
