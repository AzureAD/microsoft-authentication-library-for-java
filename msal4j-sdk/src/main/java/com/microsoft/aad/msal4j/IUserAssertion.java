// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing a delegated user identity used by downstream applications in the On-Behalf-Of flow.
 * <p>
 * The On-Behalf-Of flow is used when a service receives a token from a client application and needs to
 * call another service on behalf of the original user. In this scenario, the received token serves as
 * the user assertion that proves the original user's identity and authentication.
 * <p>
 * This interface is typically used with {@link OnBehalfOfParameters} when acquiring tokens in
 * middle-tier applications that need to call downstream services.
 */
public interface IUserAssertion {

    /**
     * Gets the assertion token used in the On-Behalf-Of flow.
     * <p>
     * The assertion is typically a JWT token received from an upstream client application
     * that represents the original user's identity and authentication.
     *
     * @return The assertion token as a string value, usually a JWT.
     */
    String getAssertion();

    /**
     * Gets a hash of the assertion token.
     * <p>
     * This hash is used as a key for caching tokens acquired via the On-Behalf-Of flow,
     * allowing the application to efficiently retrieve tokens for the same user assertion
     * without making redundant requests to the token service.
     *
     * @return Base64 encoded SHA256 hash of the assertion token.
     */
    String getAssertionHash();
}
