// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.concurrent.CompletableFuture;

/**
 * Interface representing a confidential client application (Web App, Web API, Daemon App).
 * Confidential client applications are trusted to safely store application secrets, and therefore
 * can be used to acquire tokens in then name of either the application or an user.
 * For details see https://aka.ms/msal4jclientapplications
 */
public interface IConfidentialClientApplication extends IClientApplicationBase {
    /**
     * @return a boolean value which determines whether x5c claim (public key of the certificate)
     * will be sent to the STS.
     */
    boolean sendX5c();

    /**
     * Acquires tokens from the authority configured in the application, for the confidential client
     * itself. It will by default attempt to get tokens from the token cache. If no tokens are found,
     * it falls back to acquiring them via client credentials from the STS
     * @param parameters instance of {@link ClientCredentialParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(ClientCredentialParameters parameters);

    /**
     * Acquires an access token for this application (usually a Web API) from the authority configured
     * in the application, in order to access another downstream protected Web API on behalf of a user
     * using the On-Behalf-Of flow. This confidential client application was itself called with a token
     * which will be provided in the {@link UserAssertion} to the {@link OnBehalfOfParameters}
     * @param parameters instance of {@link OnBehalfOfParameters}
     * @return {@link CompletableFuture} containing an {@link IAuthenticationResult}
     */
    CompletableFuture<IAuthenticationResult> acquireToken(OnBehalfOfParameters parameters);
}
