// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Provides a signed client assertion and its optional token binding certificate.
 */
@FunctionalInterface
public interface IClientSignedAssertionProvider {

    /**
     * Produces signed assertion material for a confidential-client token request.
     *
     * @param options context for the token request
     * @return signed assertion material
     */
    ClientSignedAssertion getAssertion(AssertionRequestOptions options) throws Exception;
}
