// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * A list of possible sources for the tokens found in an {@link IAuthenticationResult}
 */
public enum TokenSource {

    /**
     * Indicates tokens came from an identity provider, such as Azure AD
     */
    IDENTITY_PROVIDER,

    /**
     * Indicates tokens came from MSAL's cache
     */
    CACHE
}