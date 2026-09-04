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
    void deserializedMtlsResultFailsClosedWithoutBindingContext() throws Exception {
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
        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(serialized))) {
            InvalidObjectException exception = assertThrows(
                    InvalidObjectException.class,
                    input::readObject);
            assertTrue(exception.getMessage().contains("process-local binding context"));
        }
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
        assertEquals(MtlsBindingStrength.NONE,
                bearer.mtlsBindingStrength());
        assertEquals(MtlsBindingStrength.KEY_GUARD,
                first.mtlsBindingStrength());
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
        public MtlsBindingStrength bindingStrength() {
            return MtlsBindingStrength.KEY_GUARD;
        }

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
    }
}
