// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/***
 * Credential type containing an assertion representing a delegated user identity.
 * Used as a parameter in {@link OnBehalfOfParameters}
 */
public class UserAssertion implements IUserAssertion {

    private final String assertion;
    private final String assertionHash;

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
        this.assertionHash = StringHelper.createBase64EncodedSha256Hash(this.assertion);
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

    /**
     * @return Base64 encoded SHA256 hash of the assertion
     */
    public String getAssertionHash() {
        return this.assertionHash;
    }
}
