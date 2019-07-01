// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Interface representing user credential used by downstream application in On-Behalf-Of flow
 */
public interface IUserAssertion {

    /**
     * Gets the assertion.
     *
     * @return string value
     */
    String getAssertion();
}
