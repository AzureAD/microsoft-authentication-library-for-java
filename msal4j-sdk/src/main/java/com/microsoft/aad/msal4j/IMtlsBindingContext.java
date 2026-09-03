// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509ExtendedKeyManager;
import java.security.cert.X509Certificate;

/**
 * Process-local mTLS binding capability associated with an mTLS PoP access token.
 *
 * <p>The private key is not exportable. Applications can reuse the returned
 * {@link SSLContext} for ordinary JSSE resource calls that use JVM default trust.
 * Applications that require custom trust or a non-JSSE transport can use the returned
 * {@link X509ExtendedKeyManager} to build the appropriate TLS context.</p>
 */
public interface IMtlsBindingContext {

    /**
     * Returns the strength actually used by this binding context.
     */
    default MtlsBindingStrength bindingStrength() {
        return MtlsBindingStrength.SOFTWARE;
    }

    /**
     * Returns a stable, ready-to-use JSSE context for this binding generation.
     * The context presents {@link #bindingCertificate()} and uses the JVM's default
     * trust managers.
     *
     * <p>It is safe to reuse this context across resource calls. Applications that
     * require custom trust anchors should instead initialize their own context with
     * {@link #keyManager()}.</p>
     */
    SSLContext sslContext();

    /**
     * Returns the stable key manager backed by the non-exportable binding key.
     *
     * <p>Applications that require custom trust anchors or a non-JSSE transport can
     * combine this key manager with their own trust configuration. The key manager
     * always selects the binding certificate for compatible RSA client-auth requests,
     * independent of issuer hints from the server.</p>
     */
    X509ExtendedKeyManager keyManager();

    X509Certificate bindingCertificate();

    /**
     * Base64URL-without-padding SHA-256 digest of the complete leaf certificate DER.
     */
    String keyId();
}
