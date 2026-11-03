// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Native;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class AttestationLibraryLoaderTest {

    @TempDir
    Path directory;

    @Test
    void nativeLibraryIsNotBundled() {
        assertNull(AttestationLibraryLoader.class.getResource(
                "/META-INF/native/win-x64/AttestationClientLib.dll"));
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @EnabledIfEnvironmentVariable(named = "MSAL_KEY_ATTESTATION_LIBRARY_PATH", matches = ".+")
    void applicationSuppliedLibraryLoadsWhenConfigured() {
        assertNotNull(AttestationLibraryLoader.load());
    }

    @Test
    void missingConfigurationExplainsApplicationResponsibility() {
        for (String path : new String[]{null, "", " "}) {
            MtlsMsiException error = assertThrows(MtlsMsiException.class,
                    () -> AttestationLibraryLoader.resolveLibraryPath(path));
            assertTrue(error.getMessage().contains(AttestationLibraryLoader.LIBRARY_PATH_ENV));
            assertTrue(error.getMessage().contains("does not bundle or download"));
        }
    }

    @Test
    void invalidRelativeMissingAndDirectoryPathsFailClosed() {
        for (String path : new String[]{
                "\0", "AttestationClientLib.dll", directory.resolve("missing.dll").toString(),
                directory.toString()}) {
            assertThrows(MtlsMsiException.class,
                    () -> AttestationLibraryLoader.resolveLibraryPath(path));
        }
    }

    @Test
    void applicationLibraryIsResolvedWithoutCopyingOrDeletingIt() throws Exception {
        Path library = Files.createFile(directory.resolve("AttestationClientLib.dll"));
        assertEquals(library.toRealPath(),
                AttestationLibraryLoader.resolveLibraryPath(library.toString()));
        assertTrue(Files.exists(library));
    }

    @Test
    void verifiesSignatureBeforeLoadingTheExactConfiguredPath() throws Exception {
        Path library = Files.createFile(directory.resolve("AttestationClientLib.dll")).toRealPath();
        AttestationLibrary expected = mock(AttestationLibrary.class);
        try (MockedStatic<AuthenticodeVerifier> verifier = mockStatic(AuthenticodeVerifier.class);
             MockedStatic<Native> nativeApi = mockStatic(Native.class)) {
            nativeApi.when(() -> Native.load(library.toString(), AttestationLibrary.class))
                    .thenAnswer(invocation -> {
                        verifier.verify(() -> AuthenticodeVerifier.verify(library));
                        return expected;
                    });
            assertSame(expected, AttestationLibraryLoader.loadLibrary(library));
            nativeApi.verify(() -> Native.load(library.toString(), AttestationLibrary.class));
        }
        assertTrue(Files.exists(library));
    }

    @Test
    void invalidSignatureNeverLoadsOrDeletesTheApplicationLibrary() throws Exception {
        Path library = Files.createFile(directory.resolve("AttestationClientLib.dll"));
        try (MockedStatic<AuthenticodeVerifier> verifier = mockStatic(AuthenticodeVerifier.class);
             MockedStatic<Native> nativeApi = mockStatic(Native.class)) {
            verifier.when(() -> AuthenticodeVerifier.verify(library))
                    .thenThrow(new MtlsMsiException("Invalid signature"));
            assertThrows(MtlsMsiException.class, () -> AttestationLibraryLoader.loadLibrary(library));
            nativeApi.verifyNoInteractions();
        }
        assertTrue(Files.exists(library));
    }

    @Test
    void nativeLoadFailureHasAnActionableErrorAndNoFallback() throws Exception {
        Path library = Files.createFile(directory.resolve("AttestationClientLib.dll"));
        try (MockedStatic<AuthenticodeVerifier> verifier = mockStatic(AuthenticodeVerifier.class);
             MockedStatic<Native> nativeApi = mockStatic(Native.class)) {
            nativeApi.when(() -> Native.load(library.toString(), AttestationLibrary.class))
                    .thenThrow(new UnsatisfiedLinkError("Missing native dependency"));
            MtlsMsiException error = assertThrows(MtlsMsiException.class,
                    () -> AttestationLibraryLoader.loadLibrary(library));
            assertTrue(error.getMessage().contains(AttestationLibraryLoader.LIBRARY_PATH_ENV));
            assertInstanceOf(UnsatisfiedLinkError.class, error.getCause());
            nativeApi.verify(() -> Native.load(library.toString(), AttestationLibrary.class));
            nativeApi.verifyNoMoreInteractions();
        }
    }
}
