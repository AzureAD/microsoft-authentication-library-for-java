// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents the certificate that an {@link TokenType#MTLS_POP} access token is bound to.
 *
 * <p>This type exposes <b>public certificate material only</b> — the X.509 certificate chain
 * ({@code x5c}) and the SHA-256 thumbprint ({@code x5t#S256}) of the leaf certificate. It never
 * exposes the private key.
 *
 * <p>Instances are available via {@link AuthenticationResultMetadata#bindingCertificate()} for
 * mTLS Proof-of-Possession results, and are {@code null} for Bearer results. For more details, see
 * https://aka.ms/msal4j-pop
 */
public final class BindingCertificate implements Serializable {

    private static final long serialVersionUID = 1L;

    private final List<X509Certificate> certificateChain;
    private final String thumbprintSha256;

    BindingCertificate(List<X509Certificate> certificateChain, String thumbprintSha256) {
        this.certificateChain = certificateChain == null
                ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(certificateChain));
        this.thumbprintSha256 = thumbprintSha256;
    }

    /**
     * @return the X.509 certificate chain ({@code x5c}) the token is bound to. The leaf certificate is
     * first. Public material only — never contains a private key.
     */
    public List<X509Certificate> certificateChain() {
        return certificateChain;
    }

    /**
     * @return the base64url-encoded SHA-256 thumbprint ({@code x5t#S256}) of the leaf certificate the
     * token is bound to.
     */
    public String thumbprintSha256() {
        return thumbprintSha256;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BindingCertificate)) return false;

        BindingCertificate other = (BindingCertificate) o;

        if (!Objects.equals(thumbprintSha256, other.thumbprintSha256)) return false;
        return Objects.equals(certificateChain, other.certificateChain);
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = result * 59 + (this.thumbprintSha256 == null ? 43 : this.thumbprintSha256.hashCode());
        result = result * 59 + (this.certificateChain == null ? 43 : this.certificateChain.hashCode());
        return result;
    }
}
