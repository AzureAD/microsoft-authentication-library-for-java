// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ManagedIdentityMtlsBindingTest {

    @Test
    void tokenEndpointMustUseHttps() {
        IMtlsBindingContext context = new IMtlsBindingContext() {
            @Override
            public SSLContext sslContext() {
                return null;
            }

            @Override
            public javax.net.ssl.X509ExtendedKeyManager keyManager() {
                return null;
            }

            @Override
            public X509Certificate bindingCertificate() {
                return null;
            }

            @Override
            public String keyId() {
                return "key";
            }
        };

        assertThrows(IllegalArgumentException.class,
                () -> new ManagedIdentityMtlsBinding(
                        context, "client", "http://login.example/token"));
        assertThrows(IllegalArgumentException.class,
                () -> new ManagedIdentityMtlsBinding(
                        context, "client", "not-a-url"));
    }
}
