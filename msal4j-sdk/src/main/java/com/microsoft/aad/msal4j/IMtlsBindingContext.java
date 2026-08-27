// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.cert.X509Certificate;

/**
 * Process-local mTLS binding capability associated with an mTLS PoP access token.
 *
 * <p>The private key is not exportable. Applications can use the returned
 * {@link SSLContext} with a Java JSSE HTTP stack, or use the returned
 * {@link X509ExtendedKeyManager} to build a transport-specific TLS context.</p>
 */
public interface IMtlsBindingContext {

    /**
     * Returns the strength actually used by this binding context.
     */
    default MtlsBindingStrength bindingStrength() {
        return MtlsBindingStrength.SOFTWARE;
    }

    /**
     * Returns a ready-to-use JSSE context. The context uses the JVM's default trust
     * managers.
     */
    SSLContext sslContext();

    /**
     * Returns the key manager backed by the non-exportable binding key.
     *
     * <p>Applications that require custom trust anchors or a transport-specific TLS
     * context can combine this key manager with their own trust configuration.</p>
     */
    X509ExtendedKeyManager keyManager();

    X509Certificate bindingCertificate();

    /**
     * Base64URL-without-padding SHA-256 digest of the complete leaf certificate DER.
     */
    String keyId();
}
