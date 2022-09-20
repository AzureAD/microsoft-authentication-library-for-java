// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;

/**
 * Used to define the basic set of methods that all Brokers must implement
 *
 * All methods are so they can be referenced by MSAL Java without an implementation, and by default simply throw an
 * exception saying that a broker implementation is missing
 */
public interface IBroker {

    /**
     * Called when the 'useBroker' flag is first set, and will perform any initialization needed to make the broker available for future calls
     *
     * Will also detect dependency or accessibility errors with the Broker package, allowing an exception to provide the error to the app developer
     * before any actual calls to a broker are made
     */
    default void initializeBroker(){
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token silently, i.e. without direct user interaction
     *
     * This may be accomplished by returning tokens from a token cache, using cached refresh tokens to get new tokens,
     * or via any authentication flow where a user is not prompted to enter credentials
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, SilentParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token silently, i.e. without direct user interaction, using a refresh token
     *
     * This requires the broker implementation to cache the refresh tokens, either in its own cache or by populating MSAL Java's cache,
     * or otherwise have some way of retrieving refresh tokens cached somewhere else
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, RefreshTokenParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token interactively, by prompting users to enter their credentials in some way
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, InteractiveRequestParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Acquire a token silently, i.e. without direct user interaction, using username/password authentication
     *
     * @param requestParameters MsalRequest object which contains everything needed for the broker implementation to make a request
     * @return IBroker implementations will return an AuthenticationResult object
     */
    default IAuthenticationResult acquireToken(PublicClientApplication application, UserNamePasswordParameters requestParameters) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    /**
     * Retrieve an account information from the broker based on an account ID
     *
     * @return IAccount implementations will return an Account object
     */
    default Account getAccount(String id) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }

    //TODO: Not needed? ClientApplication has getAccounts(), but that would rely on us caching the accounts, which MSALRuntime also caches
    default Set<Account> getAccounts() {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }
}
