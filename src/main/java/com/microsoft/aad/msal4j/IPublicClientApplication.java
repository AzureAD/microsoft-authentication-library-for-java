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

import java.util.concurrent.CompletableFuture;

public interface IPublicClientApplication extends IClientApplicationBase {

    /**
     * Acquires a security token using a username/password flow.
     *
     * @param parameters#scopes   scopes of the access request
     * @param parameters#username Username of the managed or federated user.
     * @param parameters#password Password of the managed or federated user.
     *                            If null, integrated authentication will be used.
     * @return A {@link CompletableFuture} object representing the
     * {@link IAuthenticationResult} of the call. It contains Access
     * Token, Refresh Token and the Access Token's expiration time.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(UserNamePasswordParameters parameters);

    /**
     * Acquires a security token using Windows integrated authentication flow.
     *
     * @param parameters#scopes   scopes of the access request
     * @param parameters#username Username of the managed or federated user.
     * @return A {@link CompletableFuture} object representing the
     * {@link IAuthenticationResult} of the call. It contains Access
     * Token, Refresh Token and the Access Token's expiration time.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(IntegratedWindowsAuthenticationParameters parameters);

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
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call.
     * It contains AccessTokenCacheEntity, Refresh Token and the Access Token's expiration time.
     * @throws MsalException thrown if authorization is pending or another error occurred.
     *                                 If the errorCode of the exception is AuthenticationErrorCode.AUTHORIZATION_PENDING,
     *                                 the call needs to be retried until the AccessToken is returned.
     *                                 DeviceCode.interval - The minimum amount of time in seconds that the client
     *                                 SHOULD wait between polling requests to the token endpoint
     */
    CompletableFuture<IAuthenticationResult> acquireToken(DeviceCodeFlowParameters parameters);
}
