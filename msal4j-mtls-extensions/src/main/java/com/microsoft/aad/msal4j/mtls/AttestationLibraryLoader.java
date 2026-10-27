// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import com.sun.jna.Native;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

final class AttestationLibraryLoader {

    static final String LIBRARY_PATH_ENV = "MSAL_KEY_ATTESTATION_LIBRARY_PATH";

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
            if (library == null) {
                Path path = resolveLibraryPath(System.getenv(LIBRARY_PATH_ENV));
                String architecture = System.getProperty("os.arch", "");
                if (!System.getProperty("os.name", "").toLowerCase(Locale.ROOT).startsWith("windows")
                        || (!"amd64".equalsIgnoreCase(architecture)
                        && !"x86_64".equalsIgnoreCase(architecture))) {
                    throw new MtlsMsiException(
                            "KeyGuard attestation requires Windows x64; detected architecture: "
                                    + architecture);
                }
                library = loadLibrary(path);
                loadedLibrary = library;
            }
            return library;
        }
    }

    static Path resolveLibraryPath(String configuredPath) {
        if (configuredPath == null || configuredPath.trim().isEmpty()) {
            throw new MtlsMsiException(
                    "Set " + LIBRARY_PATH_ENV + " to the absolute path of an application-supplied "
                            + "AttestationClientLib.dll from Microsoft.Azure.Security.KeyGuardAttestation. "
                            + "MSAL does not bundle or download the native package.");
        }
        try {
            Path path = Paths.get(configuredPath);
            if (!path.isAbsolute()) {
                throw new MtlsMsiException(LIBRARY_PATH_ENV + " must be an absolute DLL path.");
            }
            path = path.toRealPath();
            if (!Files.isRegularFile(path) || !Files.isReadable(path)) {
                throw new MtlsMsiException(LIBRARY_PATH_ENV + " must identify a readable DLL file.");
            }
            return path;
        } catch (InvalidPathException | IOException e) {
            throw new MtlsMsiException(
                    "Cannot access the application-supplied attestation DLL. Check "
                            + LIBRARY_PATH_ENV + " and the application's file permissions.", e);
        }
    }

    static AttestationLibrary loadLibrary(Path library) {
        try {
            AuthenticodeVerifier.verify(library);
            return Native.load(library.toString(), AttestationLibrary.class);
        } catch (UnsatisfiedLinkError e) {
            throw new MtlsMsiException(
                    "Could not load the application-supplied attestation DLL. Check "
                            + LIBRARY_PATH_ENV + ", Windows x64 compatibility and native dependencies.", e);
        }
    }
}
