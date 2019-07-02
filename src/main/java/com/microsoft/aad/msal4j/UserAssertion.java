// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/***
 * Credential type containing an assertion representing a delegated user identity.
 * Used as a parameter in {@link OnBehalfOfParameters}
 */
public class UserAssertion implements IUserAssertion {

    private final String assertion;

    /**
     * Constructor to create credential with a jwt token encoded as a base64 url
     * encoded string.
     *
     * @param assertion
     *            The jwt used as credential.
     */
    public UserAssertion(final String assertion) {
        if (StringHelper.isBlank(assertion)) {
            throw new NullPointerException("assertion");
        }

        this.assertion = assertion;
    }

    /**
     * Gets the assertion.
     *
     * @return assertion
     */
    @Override
    public String getAssertion() {
        return assertion;
    }
}
