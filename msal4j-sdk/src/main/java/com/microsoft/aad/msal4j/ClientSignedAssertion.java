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
 * once during client-credentials preflight and again when a network request is sent. Preflight
 * selects the TLS certificate and cache binding. A certificate cannot first appear in the
 * network callback. Without mTLS PoP, the network callback may omit the certificate to select
 * {@code jwt-bearer} while retaining preflight's mTLS transport. With mTLS PoP, both invocations
 * must return a certificate. Providers must keep the runtime assertion consistent with the
 * preflight certificate; MSAL does not compare certificate identities across invocations.</p>
 *
 * <p>Certificate-bound results are supported only for client-credentials requests. Other grants
 * may use this provider only without a binding certificate and do not invoke preflight.</p>
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
