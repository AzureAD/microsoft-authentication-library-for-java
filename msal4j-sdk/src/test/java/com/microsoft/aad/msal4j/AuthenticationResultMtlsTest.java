// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationResultMtlsTest {

    @Test
    void bindingContextIsProcessLocalAndNotSerialized() throws Exception {
        IMtlsBindingContext context = new TestBindingContext();
        AuthenticationResult result = AuthenticationResult.builder()
                .accessToken("secret")
                .expiresOn(System.currentTimeMillis() / 1000 + 3600)
                .tokenType("mtls_pop")
                .mtlsBindingContext(context)
                .build();

        byte[] serialized;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(result);
            serialized = bytes.toByteArray();
        }
        AuthenticationResult restored;
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(serialized))) {
            restored = (AuthenticationResult) input.readObject();
        }

        assertEquals("mtls_pop", restored.tokenType());
        assertNull(restored.mtlsBindingContext());
        assertNull(restored.bindingCertificate());
    }

    @Test
    void equalityIncludesTokenTypeButExcludesLiveBindingContext() {
        AuthenticationResult bearer = result("Bearer", null);
        AuthenticationResult first = result("mtls_pop", new TestBindingContext());
        AuthenticationResult second =
                first.withMtlsBindingContext(new TestBindingContext());

        assertNotEquals(bearer, first);
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    private static AuthenticationResult result(
            String tokenType,
            IMtlsBindingContext context) {
        return AuthenticationResult.builder()
                .accessToken("secret")
                .expiresOn(123)
                .tokenType(tokenType)
                .isPopAuthorization(context == null ? null : Boolean.TRUE)
                .mtlsBindingContext(context)
                .build();
    }

    private static final class TestBindingContext implements IMtlsBindingContext {
        @Override
        public SSLContext sslContext() {
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
    }
}
