// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;


/**
 * Representation of client credential containing a secret in string format
 */
@EqualsAndHashCode
public final class ClientSecret implements IClientCredential {


    @Accessors(fluent = true)
    @Getter
    private final String clientSecret;

     /**
     * Constructor to create credential with client id and secret
     *
     * @param clientSecret
     *            Secret of the client requesting the token.
     */
    ClientSecret(final String clientSecret) {
        if (StringHelper.isBlank(clientSecret)) {
            throw new IllegalArgumentException("clientSecret is null or empty");
        }

        this.clientSecret = clientSecret;
    }
}
