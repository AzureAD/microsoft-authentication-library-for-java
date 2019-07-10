// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * The exception type thrown when a claims challenge error occurs during token acquisition.
 */
public class ClaimsChallengeException extends AuthenticationException {

    /**
     * claims challenge value
     */
    @Accessors(fluent = true)
    @Getter
    private final String claims;

    /**
     * Constructor
     *
     * @param message string error message
     * @param claims claims challenge returned from the STS
     */
    public ClaimsChallengeException(String message, String claims) {
        super(message);

        this.claims = claims;
    }
}
