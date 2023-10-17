// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * Contains metadata and additional context for the contents of an AuthenticationResult
 */
@Accessors(fluent = true)
@Getter
@Setter(AccessLevel.PACKAGE)
public class AuthenticationResultMetadata implements Serializable {

    private TokenSource tokenSource;

    /**
     * Sets default metadata values. Used when creating an {@link IAuthenticationResult} before the values are known.
     */
    AuthenticationResultMetadata() {
    }

    AuthenticationResultMetadata(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }
}