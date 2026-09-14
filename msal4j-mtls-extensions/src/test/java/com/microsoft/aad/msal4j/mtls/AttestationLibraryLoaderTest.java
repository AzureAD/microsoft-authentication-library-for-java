// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AttestationLibraryLoaderTest {

    @Test
    void bundledLibraryMatchesMsalDotNetVersionAndHash() throws Exception {
        assertEquals("1.1.5", AttestationLibraryLoader.VERSION);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream resource = AttestationLibraryLoader.class.getResourceAsStream(
                AttestationLibraryLoader.RESOURCE_PATH)) {
            assertNotNull(resource);
            try (DigestInputStream input = new DigestInputStream(resource, digest)) {
                byte[] buffer = new byte[8192];
                while (input.read(buffer) != -1) {
                    // Consume the complete resource.
                }
            }
        }

        assertEquals(AttestationLibraryLoader.SHA256, toHex(digest.digest()));
    }

    @Test
    void cleanupRemovesExtractedLibraryAndDirectory() throws Exception {
        Path directory = Files.createTempDirectory("attestation-loader-test-");
        Path library = Files.createFile(
                directory.resolve("AttestationClientLib.dll"));

        AttestationLibraryLoader.cleanupExtractedLibrary(library, directory);

        assertFalse(Files.exists(library));
        assertFalse(Files.exists(directory));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}
