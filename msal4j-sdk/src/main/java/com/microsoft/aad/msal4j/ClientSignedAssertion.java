// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

/**
 * Result returned by a client signed-assertion provider.
 *
 * <p>The assertion is forwarded unchanged as the OAuth {@code client_assertion}. When a token
 * binding certificate is supplied, MSAL presents it on the token endpoint TLS connection and uses
 * the {@code jwt-pop} client assertion type. The certificate is optional unless the token request
 * also calls {@link ClientCredentialParameters.ClientCredentialParametersBuilder#mtlsProofOfPossession()}.</p>
 *
 * <p>Providers are responsible for signing and caching the assertion. MSAL can invoke the provider
 * once during request preflight and again when a network request is sent.</p>
 *
 * @see ClientCredentialFactory#createFromSignedAssertionCallback(IClientSignedAssertionProvider)
 */
public class ClientSignedAssertion {
    private String assertion;
    private IClientCertificate tokenBindingCertificate;

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public IClientCertificate getTokenBindingCertificate() {
        return tokenBindingCertificate;
    }

    public void setTokenBindingCertificate(IClientCertificate tokenBindingCertificate) {
        this.tokenBindingCertificate = tokenBindingCertificate;
    }
}
