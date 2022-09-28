// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Used to define the basic set of methods that all Brokers must implement
 *
 * All methods are so they can be referenced by MSAL Java without an implementation, and by default simply throw an
 * exception saying that a broker implementation is missing
 */
public interface IBroker {

    /**
     * checks if a IBroker implementation exists
     */

    boolean isAvailable;
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

    default CompletableFuture removeAccount(IAccount account) {
        throw new MsalClientException("Broker implementation missing", AuthenticationErrorCode.MISSING_BROKER);
    }
}