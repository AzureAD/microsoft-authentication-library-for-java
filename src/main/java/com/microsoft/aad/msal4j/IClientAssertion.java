// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Credential type containing an assertion of type
 * "urn:ietf:params:oauth:token-type:jwt".
 */
public interface IClientAssertion extends IClientCredential{

    /**
     * @return Jwt token encoded as a base64 URL encoded string
     */
    String assertion();
}
