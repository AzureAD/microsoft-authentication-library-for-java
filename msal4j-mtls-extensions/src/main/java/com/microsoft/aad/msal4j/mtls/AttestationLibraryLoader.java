// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Native;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.nio.file.attribute.UserPrincipal;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.EnumSet;

final class AttestationLibraryLoader {

    static final String VERSION = "1.1.5";
    static final String RESOURCE_PATH =
            "/META-INF/native/win-x64/AttestationClientLib.dll";
    static final String SHA256 =
            "90dfcce20e1a74519b49796eeee17e6e59a257c3acf754f454a49380d28a568b";

    private static final Object LOAD_LOCK = new Object();
    private static volatile AttestationLibrary loadedLibrary;

    private AttestationLibraryLoader() {
    }

    static AttestationLibrary load() throws MtlsMsiException {
        AttestationLibrary library = loadedLibrary;
        if (library != null) {
            return library;
        }

        synchronized (LOAD_LOCK) {
            library = loadedLibrary;
            if (library != null) {
                return library;
            }

            Path extractedLibrary = extractBundledLibrary();
            try {
                library = Native.load(
                        extractedLibrary.toAbsolutePath().toString(),
                        AttestationLibrary.class);
            } catch (UnsatisfiedLinkError e) {
                throw new MtlsMsiException(
                        "Could not load bundled Microsoft.Azure.Security.KeyGuardAttestation " +
                                VERSION + " native library: " + e.getMessage(),
                        e);
            }
            loadedLibrary = library;
            return library;
        }
    }

    static Path extractBundledLibrary() throws MtlsMsiException {
        String architecture = System.getProperty("os.arch", "");
        if (!"amd64".equalsIgnoreCase(architecture)
                && !"x86_64".equalsIgnoreCase(architecture)) {
            throw new MtlsMsiException(
                    "Microsoft.Azure.Security.KeyGuardAttestation " + VERSION +
                            " is bundled only for Windows x64; detected architecture: " +
                            architecture);
        }

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new MtlsMsiException("SHA-256 is unavailable in this Java runtime.", e);
        }

        Path directory = null;
        Path library = null;
        try (InputStream resource =
                     AttestationLibraryLoader.class.getResourceAsStream(RESOURCE_PATH)) {
            if (resource == null) {
                throw new MtlsMsiException(
                        "Bundled Microsoft.Azure.Security.KeyGuardAttestation " + VERSION +
                                " native library is missing from the extension JAR.");
            }

            directory = Files.createTempDirectory("msal4j-keyguard-attestation-");
            restrictToCurrentUser(directory);
            library = directory.resolve("AttestationClientLib.dll");
            try (DigestInputStream verifiedResource =
                         new DigestInputStream(resource, digest)) {
                Files.copy(verifiedResource, library);
            }

            String actualHash = toHex(digest.digest());
            if (!SHA256.equals(actualHash)) {
                Files.deleteIfExists(library);
                Files.deleteIfExists(directory);
                throw new MtlsMsiException(
                        "Bundled Microsoft.Azure.Security.KeyGuardAttestation " + VERSION +
                                " failed SHA-256 verification.");
            }

            AuthenticodeVerifier.verify(library);

            File directoryFile = directory.toFile();
            File libraryFile = library.toFile();
            directoryFile.deleteOnExit();
            libraryFile.deleteOnExit();
            return library;
        } catch (IOException e) {
            if (library != null) {
                try {
                    Files.deleteIfExists(library);
                } catch (IOException ignored) {
                    // Preserve the original extraction failure.
                }
            }
            if (directory != null) {
                try {
                    Files.deleteIfExists(directory);
                } catch (IOException ignored) {
                    // Preserve the original extraction failure.
                }
            }
            throw new MtlsMsiException(
                    "Could not extract bundled Microsoft.Azure.Security.KeyGuardAttestation " +
                            VERSION + " native library.",
                    e);
        }
    }

    private static void restrictToCurrentUser(Path directory) throws IOException {
        AclFileAttributeView aclView = Files.getFileAttributeView(
                directory,
                AclFileAttributeView.class);
        if (aclView == null) {
            throw new IOException(
                    "The temporary directory does not support Windows ACLs.");
        }

        UserPrincipal owner = Files.getOwner(directory);
        AclEntry ownerAccess = AclEntry.newBuilder()
                .setType(AclEntryType.ALLOW)
                .setPrincipal(owner)
                .setPermissions(EnumSet.allOf(AclEntryPermission.class))
                .build();
        aclView.setAcl(Collections.singletonList(ownerAccess));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            result.append(String.format("%02x", value & 0xff));
        }
        return result.toString();
    }
}
