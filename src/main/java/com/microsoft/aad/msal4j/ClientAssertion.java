// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.nimbusds.oauth2.sdk.auth.JWTAuthentication;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Credential type containing an assertion of type
 * "urn:ietf:params:oauth:token-type:jwt".
 */
@Accessors(fluent = true)
@Getter
@EqualsAndHashCode
final class ClientAssertion implements IClientCredential{

    public static final String assertionType = JWTAuthentication.CLIENT_ASSERTION_TYPE;
    private final String assertion;

    /**
     * Constructor to create credential with a jwt token encoded as a base64 url
     * encoded string.
     *
     * @param assertion The jwt used as credential.
     */
    ClientAssertion(final String assertion) {
        if (StringHelper.isBlank(assertion)) {
            throw new NullPointerException("assertion");
        }

        this.assertion = assertion;
    }
}