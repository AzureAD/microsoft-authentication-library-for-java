// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;

/**
 * Process-local mTLS binding capability associated with an mTLS PoP access token.
 *
 * <p>The private key is not exportable. Applications use the returned {@link SSLContext}
 * with a Java JSSE HTTP stack when calling the downstream resource.</p>
 */
public interface IMtlsBindingContext {

    SSLContext sslContext();

    X509Certificate bindingCertificate();

    /**
     * Base64URL-without-padding SHA-256 digest of the complete leaf certificate DER.
     */
    String keyId();
}
