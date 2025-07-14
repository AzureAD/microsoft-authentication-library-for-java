// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Credential type containing an assertion of type
 * "urn:ietf:params:oauth:token-type:jwt".
 * <p>
 * Client assertions provide a way for confidential client applications to authenticate themselves
 * to the Microsoft identity platform without sending a client secret. Instead, the application
 * uses a JWT token as a credential.
 * <p>
 * For more details, see https://aka.ms/msal4j-client-credentials
 */
public interface IClientAssertion extends IClientCredential {

    /**
     * Gets the JWT token used for client authentication.
     *
     * @return Jwt token encoded as a base64 URL encoded string
     */
    String assertion();
}
