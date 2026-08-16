// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import java.util.Collections;

/**
 * A {@link Provider} that routes RSA signature operations for {@link CngRsaPrivateKey}
 * keys through Windows CNG ({@code NCryptSignHash}) via JNA.
 *
 * <p>Install once per JVM via {@link #installIfAbsent()} before creating an
 * {@code SSLContext} that uses a {@link CngRsaPrivateKey}. JSSE will call
 * {@code Signature.getInstance("SHA256withRSA")} or
 * {@code Signature.getInstance("RSASSA-PSS")}; with this provider at
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

        putSignature("SHA256withRSA", CngSignatureSpi.Sha256WithRsa.class,
                "SHA-256withRSA");
        putSignature("SHA384withRSA", CngSignatureSpi.Sha384WithRsa.class,
                "SHA-384withRSA");
        putSignature("SHA512withRSA", CngSignatureSpi.Sha512WithRsa.class,
                "SHA-512withRSA");
        putSignature("RSASSA-PSS", CngSignatureSpi.RsaSsaPss.class,
                "SHA256withRSAandMGF1", "SHA384withRSAandMGF1", "SHA512withRSAandMGF1");
    }

    private void putSignature(
            String algorithm,
            Class<? extends CngSignatureSpi> implementation,
            String... aliases) {
        putService(new CngSignatureService(
                this,
                algorithm,
                implementation.getName(),
                Arrays.asList(aliases)));
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

    private static final class CngSignatureService extends Provider.Service {

        CngSignatureService(
                Provider provider,
                String algorithm,
                String className,
                java.util.List<String> aliases) {
            super(provider, "Signature", algorithm, className, aliases,
                    Collections.<String, String>emptyMap());
        }

        @Override
        public boolean supportsParameter(Object parameter) {
            return parameter instanceof CngRsaPrivateKey;
        }
    }
}
