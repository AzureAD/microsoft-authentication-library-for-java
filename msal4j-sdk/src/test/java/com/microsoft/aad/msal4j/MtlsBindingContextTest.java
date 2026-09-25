// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MtlsBindingContextTest {

    @Test
    void rejectsPrivateKeyThatDoesNotMatchLeafCertificate() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair mismatchedKeyPair = generator.generateKeyPair();

        assertThrows(MsalClientException.class,
                () -> MtlsBindingContext.create(certificate(mismatchedKeyPair.getPrivate())));
    }

    @Test
    void readsPrivateKeyOnceAndInstallsTheVerifiedInstance() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        PrivateKey replacementKey = generator.generateKeyPair().getPrivate();
        PrivateKey expectedKey = TestHelper.getPrivateKey();
        AtomicInteger reads = new AtomicInteger();
        IClientCertificate changingCertificate = new IClientCertificate() {
            @Override
            public PrivateKey privateKey() {
                return reads.getAndIncrement() == 0 ? expectedKey : replacementKey;
            }

            @Override
            public String publicCertificateHash() {
                return null;
            }

            @Override
            public java.util.List<String> getEncodedPublicKeyCertificateChain()
                    throws CertificateEncodingException {
                return Collections.singletonList(Base64.getEncoder().encodeToString(
                        TestHelper.getX509Cert().getEncoded()));
            }
        };

        MtlsBindingContext context = MtlsBindingContext.create(changingCertificate);

        assertEquals(1, reads.get());
        assertSame(expectedKey, context.keyManager().getPrivateKey("msal-mtls-binding"));
    }

    @Test
    void keyManagerHonorsAcceptableIssuersForEnumerationSocketAndEngine() {
        MtlsBindingContext context = MtlsBindingContext.create(
                certificate(TestHelper.getPrivateKey()));
        String alias = "msal-mtls-binding";
        X500Principal acceptable = TestHelper.getX509Cert().getIssuerX500Principal();
        X500Principal unacceptable = new X500Principal("CN=unacceptable");

        assertArrayEquals(new String[]{alias},
                context.keyManager().getClientAliases("RSA", new X500Principal[]{acceptable}));
        assertEquals(alias, context.keyManager().chooseClientAlias(
                new String[]{"RSA"}, new X500Principal[]{acceptable}, null));
        assertEquals(alias, context.keyManager().chooseEngineClientAlias(
                new String[]{"RSA"}, new X500Principal[]{acceptable},
                context.sslContext().createSSLEngine()));

        assertNull(context.keyManager().getClientAliases(
                "RSA", new X500Principal[]{unacceptable}));
        assertNull(context.keyManager().chooseClientAlias(
                new String[]{"RSA"}, new X500Principal[]{unacceptable}, null));
        assertNull(context.keyManager().chooseEngineClientAlias(
                new String[]{"RSA"}, new X500Principal[]{unacceptable},
                context.sslContext().createSSLEngine()));
    }

    private static IClientCertificate certificate(PrivateKey privateKey) {
        return new IClientCertificate() {
            @Override
            public PrivateKey privateKey() {
                return privateKey;
            }

            @Override
            public String publicCertificateHash() {
                return null;
            }

            @Override
            public java.util.List<String> getEncodedPublicKeyCertificateChain()
                    throws CertificateEncodingException {
                return Collections.singletonList(Base64.getEncoder().encodeToString(
                        TestHelper.getX509Cert().getEncoded()));
            }
        };
    }
}
