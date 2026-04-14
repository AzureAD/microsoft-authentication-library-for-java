// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Holds the mTLS binding information: the CNG-backed private key and the IMDS-issued
 * X.509 certificate that bind together for a particular managed identity.
 */
final class MtlsBindingInfo {

    final CngRsaPrivateKey privateKey;
    final X509Certificate  certificate;
    final String           mtlsEndpoint;
    final String           clientId;
    final String           tenantId;
    final Date             expiresAt;

    MtlsBindingInfo(CngRsaPrivateKey privateKey, X509Certificate certificate,
                    String mtlsEndpoint, String clientId, String tenantId) {
        this.privateKey   = privateKey;
        this.certificate  = certificate;
        this.mtlsEndpoint = mtlsEndpoint;
        this.clientId     = clientId;
        this.tenantId     = tenantId;
        // Expire 5 minutes before the cert's notAfter, matching msal-go and MSAL.NET.
        long notAfterMs = certificate.getNotAfter().getTime();
        this.expiresAt  = new Date(notAfterMs - 5L * 60 * 1000);
    }

    boolean isExpired() {
        return new Date().after(expiresAt);
    }
}
