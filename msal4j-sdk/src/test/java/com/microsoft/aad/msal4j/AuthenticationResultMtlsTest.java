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

    @Test
    void managedIdentityHelpersPreserveOriginalResource() {
        AuthenticationResult response = AuthenticationResult.builder()
                .accessToken("secret")
                .expiresOn(123)
                .scopes("https://vault.azure.net/.default")
                .tokenType("Bearer")
                .build();
        String resource = "https://vault.azure.net";

        assertEquals(resource, response.withScopes(resource).scopes());
        assertEquals(resource,
                response.withMtlsBindingContext(new TestBindingContext(), resource)
                        .scopes());
    }

    @Test
    void resultCopiesPreserveUserAssertionHash() {
        String resource = "https://vault.azure.net";
        AuthenticationResult response = AuthenticationResult.builder()
                .accessToken("secret")
                .expiresOn(123)
                .scopes(resource)
                .userAssertionHash("assertion-hash")
                .build();

        AuthenticationResult scoped = response.withScopes(resource);
        assertEquals("assertion-hash", scoped.userAssertionHash());
        assertEquals(response, scoped);
        assertEquals(response.hashCode(), scoped.hashCode());

        AuthenticationResult bound =
                response.withMtlsBindingContext(new TestBindingContext(), resource);
        assertEquals("assertion-hash", bound.userAssertionHash());
        assertEquals("mtls_pop", bound.tokenType());
        assertEquals("assertion-hash",
                bound.withMtlsBindingContext(new TestBindingContext()).userAssertionHash());
    }

    @Test
    void equalityIncludesUserAssertionHashAlongsideTokenType() {
        AuthenticationResult.AuthenticationResultBuilder builder = AuthenticationResult.builder()
                .accessToken("secret")
                .expiresOn(123)
                .tokenType("Bearer")
                .metadata(AuthenticationResultMetadata.builder().build())
                .userAssertionHash("first-assertion");
        AuthenticationResult first = builder.build();
        AuthenticationResult same = builder.build();
        AuthenticationResult different = builder.userAssertionHash("second-assertion").build();

        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, different);
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
