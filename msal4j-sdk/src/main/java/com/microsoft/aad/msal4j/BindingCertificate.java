// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Public diagnostics for the certificate bound to an mTLS PoP token.
 */
public final class BindingCertificate implements Serializable {
    private static final long serialVersionUID = 1L;

    private final List<X509Certificate> certificateChain;
    private final String thumbprintSha256;
    private final Date notBefore;
    private final Date notAfter;

    BindingCertificate(List<X509Certificate> certificateChain, String thumbprintSha256) {
        this.certificateChain = Collections.unmodifiableList(new ArrayList<>(certificateChain));
        this.thumbprintSha256 = thumbprintSha256;
        X509Certificate leaf = certificateChain.get(0);
        this.notBefore = new Date(leaf.getNotBefore().getTime());
        this.notAfter = new Date(leaf.getNotAfter().getTime());
    }

    public List<X509Certificate> certificateChain() {
        return certificateChain;
    }

    public String thumbprintSha256() {
        return thumbprintSha256;
    }

    public Date notBefore() {
        return new Date(notBefore.getTime());
    }

    public Date notAfter() {
        return new Date(notAfter.getTime());
    }
}
