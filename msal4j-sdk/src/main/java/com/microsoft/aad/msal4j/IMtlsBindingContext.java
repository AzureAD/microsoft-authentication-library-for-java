// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

/**
 * Process-local capability required to use an mTLS-bound access token.
 */
public interface IMtlsBindingContext {

    SSLContext sslContext();

    X509Certificate bindingCertificate();

    List<X509Certificate> certificateChain();

    /**
     * Base64URL-without-padding SHA-256 digest of the complete leaf certificate DER.
     */
    String keyId();

    Date notBefore();

    Date notAfter();
}
