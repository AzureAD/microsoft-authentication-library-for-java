// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticodeVerifierTest {

    @Test
    void nonzeroWinVerifyTrustResultFailsClosed() throws Exception {
        AuthenticodeVerifier.WinTrustLibrary winTrust =
                mock(AuthenticodeVerifier.WinTrustLibrary.class);
        when(winTrust.WinVerifyTrust(any(), any(), any()))
                .thenReturn(0x800B0100);
        Path file = Files.createTempFile("unsigned-", ".dll");
        try {
            assertThrows(MtlsMsiException.class,
                    () -> AuthenticodeVerifier.verify(file, winTrust));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void successfulWinVerifyTrustResultIsAccepted() throws Exception {
        AuthenticodeVerifier.WinTrustLibrary winTrust =
                mock(AuthenticodeVerifier.WinTrustLibrary.class);
        when(winTrust.WinVerifyTrust(any(), any(), any()))
                .thenReturn(0);
        Path file = Files.createTempFile("signed-", ".dll");
        try {
            assertDoesNotThrow(
                    () -> AuthenticodeVerifier.verify(file, winTrust));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void bundledLibraryHasValidAuthenticodeSignature() throws Exception {
        Path file = Files.createTempFile("attestation-", ".dll");
        try (InputStream resource = AuthenticodeVerifierTest.class
                .getResourceAsStream(AttestationLibraryLoader.RESOURCE_PATH)) {
            if (resource == null) {
                throw new IllegalStateException("Bundled DLL is missing.");
            }
            Files.copy(resource, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            assertDoesNotThrow(() -> AuthenticodeVerifier.verify(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }
}
