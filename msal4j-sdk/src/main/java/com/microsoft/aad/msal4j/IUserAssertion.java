// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing a delegated user identity used by downstream applications in On-Behalf-Of flow
 */
public interface IUserAssertion {

    /**
     * Gets the assertion.
     *
     * @return string value
     */
    String getAssertion();

    /**
     * @return Base64 encoded SHA256 hash of the assertion
     */
    String getAssertionHash();
}
