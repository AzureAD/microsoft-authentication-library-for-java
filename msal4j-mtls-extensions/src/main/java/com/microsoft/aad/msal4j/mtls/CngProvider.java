// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.security.Provider;
import java.security.Security;

/**
 * A {@link Provider} that routes RSA signature operations for {@link CngRsaPrivateKey}
 * keys through Windows CNG ({@code NCryptSignHash}) via JNA.
 *
 * <p>Install once per JVM via {@link #installIfAbsent()} before creating an
 * {@code SSLContext} that uses a {@link CngRsaPrivateKey}. JSSE will call
 * {@code Signature.getInstance("SHA256withRSA")} (TLS 1.2) or
 * {@code Signature.getInstance("RSASSA-PSS")} (TLS 1.3); with this provider at
 * high priority, {@link CngSignatureSpi} intercepts the call and signs via
 * {@code NCryptSignHash} instead of requiring an exportable private exponent.</p>
 *
 * <p>For non-{@link CngRsaPrivateKey} keys, {@link CngSignatureSpi} automatically
 * delegates to the next available provider, so installing this provider does not
 * break other RSA signing in the same JVM.</p>
 */
public final class CngProvider extends Provider {

    private static final long serialVersionUID = 1L;
    private static final String PROVIDER_NAME    = "CNG";
    private static final double PROVIDER_VERSION = 1.0;
    private static final String PROVIDER_INFO    = "Windows CNG JNA provider for JSSE mTLS";

    public CngProvider() {
        super(PROVIDER_NAME, PROVIDER_VERSION, PROVIDER_INFO);

        put("Signature.SHA256withRSA",          CngSignatureSpi.Sha256WithRsa.class.getName());
        put("Signature.SHA1withRSA",             CngSignatureSpi.Sha1WithRsa.class.getName());
        put("Signature.RSASSA-PSS",              CngSignatureSpi.RsaSsaPss.class.getName());
        // Aliases used by some TLS implementations.
        put("Alg.Alias.Signature.SHA256withRSAandMGF1", "RSASSA-PSS");
        put("Alg.Alias.Signature.SHA-256withRSA",       "SHA256withRSA");
        put("Alg.Alias.Signature.SHA1withRSA",          "SHA1withRSA");
    }

    /**
     * Installs this provider at position 1 (highest priority) if it is not already
     * registered. Safe to call multiple times.
     */
    public static void installIfAbsent() {
        if (Security.getProvider(PROVIDER_NAME) == null) {
            Security.insertProviderAt(new CngProvider(), 1);
        }
    }
}
