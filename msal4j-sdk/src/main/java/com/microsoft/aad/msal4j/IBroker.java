// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.net.URL;
import java.util.concurrent.CompletableFuture;

/**
 * Interface defining the core functionality required for authentication brokers.
 * <p>
 * Authentication brokers provide a centralized way to handle authentication across multiple applications
 * on a device or platform. They can offer benefits such as single sign-on (SSO) between applications,
 * consistent authentication experiences, and leveraging platform security features.
 * <p>
 * All methods are marked as default so they can be referenced by MSAL Java without an implementation,
 * and most will simply throw an exception if not overridden by an IBroker implementation.
 */
public interface IBroker {

    /**
     * Acquires a token silently without user interaction.
     * <p>
     * This may be accomplished by returning tokens from a token cache, using cached refresh tokens to get new tokens,
     * or via any authentication flow where a user is not prompted to enter credentials.
     *
     * @param application The public client application requesting the token
     * @param requestParameters Parameters for the silent token request
     * @return A CompletableFuture containing the authentication result with tokens and account information
     * @throws MsalClientException If no broker implementation is available
     */
    default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, SilentParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquires a token interactively by prompting the user to enter credentials.
     * <p>
     * This method presents a user interface requesting credentials from the user and handles the
     * interactive authentication flow.
     *
     * @param application The public client application requesting the token
     * @param parameters Parameters for the interactive token request
     * @return A CompletableFuture containing the authentication result with tokens and account information
     * @throws MsalClientException If no broker implementation is available
     */
    default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, InteractiveRequestParameters parameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquires a token silently using username and password authentication.
     * <p>
     * This method enables resource owner password credentials (ROPC) flow through a broker.
     * Note that this flow is not recommended for production applications as it requires handling
     * user credentials directly.
     *
     * @param application The public client application requesting the token
     * @param parameters Parameters containing username, password and other request details
     * @return A CompletableFuture containing the authentication result with tokens and account information
     * @throws MsalClientException If no broker implementation is available
     */
    default CompletableFuture<IAuthenticationResult> acquireToken(PublicClientApplication application, UserNamePasswordParameters parameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Removes an account from the broker's token cache.
     * <p>
     * This method allows applications to sign out users and remove their tokens from the broker cache.
     *
     * @param application The public client application requesting the account removal
     * @param account The account to be removed from the token cache
     * @throws MsalClientException If no broker implementation is available
     */
    default void removeAccount(PublicClientApplication application, IAccount account) throws MsalClientException {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Checks whether a broker is available and ready to use on this machine.
     * <p>
     * Applications should call this method before attempting to use broker-specific features
     * to determine if a broker is installed and accessible.
     *
     * @return true if a broker is available for authentication, false otherwise
     * @throws MsalClientException If no broker implementation is available
     */
    default boolean isBrokerAvailable() {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * MSAL Java's AuthenticationResult requires several package-private classes that a broker implementation can't access,
     * so this helper method can be used to create AuthenticationResults from within the MSAL Java package
     */
    default IAuthenticationResult parseBrokerAuthResult(String authority, String idToken, String accessToken,
                                                        String accountId, String clientInfo,
                                                        long accessTokenExpirationTime,
                                                        boolean isPopAuthorization) {

        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder();

        try {
            if (idToken != null) {
                builder.idToken(idToken);
                if (accountId != null) {
                    IdToken idTokenObj = JsonHelper.createIdTokenFromEncodedTokenString(idToken);

                    builder.accountCacheEntity(AccountCacheEntity.create(clientInfo,
                            Authority.createAuthority(new URL(authority)), idTokenObj, null));
                }
            }
            if (accessToken != null) {
                builder.accessToken(accessToken);
                builder.expiresOn(accessTokenExpirationTime);
            }

            builder.isPopAuthorization(isPopAuthorization);

        } catch (Exception e) {
            throw new MsalClientException(String.format("Exception when converting broker result to MSAL Java AuthenticationResult: %s", e.getMessage()), AuthenticationErrorCode.MSALJAVA_BROKERS_ERROR);
        }
        return builder.build();
    }
}