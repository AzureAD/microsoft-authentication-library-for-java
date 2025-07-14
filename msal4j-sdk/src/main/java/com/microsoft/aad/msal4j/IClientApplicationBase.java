// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLSocketFactory;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Interface representing a client application that can acquire tokens from the Microsoft identity platform.
 * This interface serves as the base for both public client applications (desktop/mobile apps) and
 * confidential client applications (web apps/APIs), defining common functionality for token acquisition.
 * <p>
 * Client applications are registered in the Microsoft identity platform and have their own identity,
 * represented by an application ID (client ID).
 */
interface IClientApplicationBase extends IApplicationBase {

    /**
     * Gets the client ID (application ID) for this application.
     *
     * @return Client ID (Application ID) of the application as registered in the application registration portal
     * (portal.azure.com) and as passed in the constructor of the application
     */
    String clientId();

    /**
     * Gets the authority URL for this application.
     *
     * @return URL of the authority, or security token service (STS) from which MSAL will acquire security tokens.
     * Default value is {@link IClientApplicationBase#DEFAULT_AUTHORITY}
     */
    String authority();

    /**
     * Gets whether the authority URL should be validated against a list of known authorities.
     *
     * @return A boolean value which determines whether the authority needs to be verified against a list of known authorities.
     * When true, MSAL will validate the authority against a list of well-known authorities. Set to false only for
     * development/testing with custom authority URLs.
     */
    boolean validateAuthority();

    /**
     * Computes the URL of the authorization request letting the user sign-in and consent to the
     * application. The URL target the /authorize endpoint of the authority configured in the
     * application object.
     * <p>
     * Once the user successfully authenticates, the response should contain an authorization code,
     * which can then be passed in to {@link AbstractClientApplicationBase#acquireToken(AuthorizationCodeParameters)}
     * to be exchanged for a token.
     *
     * @param parameters {@link AuthorizationRequestUrlParameters} containing the details needed to create the authorization URL,
     *                   such as scopes, response type, and redirect URI
     * @return URL of the authorization endpoint where the user can sign-in and consent to the application
     */
    URL getAuthorizationRequestUrl(AuthorizationRequestUrlParameters parameters);

    /**
     * Acquires security token from the authority using an authorization code previously received.
     * <p>
     * This is typically used as the second step in an authorization code flow, after the user has
     * authenticated and provided consent at the authorization endpoint, resulting in an authorization code.
     *
     * @param parameters {@link AuthorizationCodeParameters} containing the authorization code and other information
     *                   required to exchange the code for tokens
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call,
     *         which contains the requested tokens and account information
     */
    CompletableFuture<IAuthenticationResult> acquireToken(AuthorizationCodeParameters parameters);

    /**
     * Acquires a security token from the authority using a refresh token previously received.
     * Can be used in migration to MSAL from ADAL, and in various integration
     * scenarios where you have a refresh token available.
     *
     * @param parameters {@link RefreshTokenParameters}
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call.
     */
    CompletableFuture<IAuthenticationResult> acquireToken(RefreshTokenParameters parameters);

    /**
     * Returns tokens from cache if present and not expired or acquires new tokens from the authority
     * by using the refresh token present in cache.
     *
     * @param parameters instance of SilentParameters
     * @return A {@link CompletableFuture} object representing the {@link IAuthenticationResult} of the call.
     * @throws MalformedURLException if authorityUrl from parameters is malformed URL
     */
    CompletableFuture<IAuthenticationResult> acquireTokenSilently(SilentParameters parameters)
            throws MalformedURLException;

    /**
     * Returns accounts in the cache
     *
     * @return set of unique accounts from cache which can be used for silent acquire token call
     */
    CompletableFuture<Set<IAccount>> getAccounts();

    /**
     * Removes IAccount from the cache
     *
     * @param account instance of Account to be removed from cache
     * @return {@link CompletableFuture} object representing account removal task.
     */
    CompletableFuture removeAccount(IAccount account);
}
