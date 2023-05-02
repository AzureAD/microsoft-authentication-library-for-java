// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;

/**
 * Interface representing a public client application (Desktop, Mobile).
 * Public client application are not trusted to safely store application secrets,
 * and therefore can only request tokens in the name of an user.
 * For details see https://aka.ms/msal4jclientapplications
 */
public interface IPublicClientApplication extends IClientApplicationBase {

    /**
     * Acquires tokens from the authority configured in the application via Username/Password authentication.
     *
     * @param parameters instance of {@link UserNamePasswordParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(UserNamePasswordParameters parameters);

    /**
     * Acquires tokens from the authority configured in the application via Integrated Windows Authentication.
     *
     * @param parameters instance of {@link IntegratedWindowsAuthenticationParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(IntegratedWindowsAuthenticationParameters parameters);

    /**
     * Acquires security token from the authority using an device code flow.
     * Flow is designed for devices that do not have access to a browser or have input constraints.
     * The authorization server issues DeviceCode object with verification code, an end-user code
     * and the end-user verification URI. DeviceCode is provided through deviceCodeConsumer callback.
     * End-user should be instructed to use another device to connect to the authorization server to approve the access request.
     * Since the client cannot receive incoming requests, it polls the authorization server repeatedly
     * until the end-user completes the approval process.
     *
     * @param parameters instance of {@link DeviceCodeFlowParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     * @throws MsalException thrown if authorization is pending or another error occurred.
     *                       If the errorCode of the exception is AuthenticationErrorCode.AUTHORIZATION_PENDING,
     *                       the call needs to be retried until the AccessToken is returned.
     *                       DeviceCode.interval - The minimum amount of time in seconds that the client
     *                       SHOULD wait between polling requests to the token endpoint
     */
    CompletableFuture<IAuthenticationResult> acquireToken(DeviceCodeFlowParameters parameters);

    /**
     * Acquires tokens from the authority using authorization code grant. Will attempt to open the
     * default system browser where the user can input the credentials interactively, consent to scopes,
     * and do multi-factor authentication if such a policy is enabled on the Azure AD tenant.
     * System browser can behavior can be customized via {@link InteractiveRequestParameters#systemBrowserOptions}.
     * For more information, see https://aka.ms/msal4j-interactive-request
     *
     * @param parameters instance of {@link InteractiveRequestParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(InteractiveRequestParameters parameters);
}
