// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j.mtls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;

/**
 * Locates {@code MsalMtlsMsiHelper.exe} using the following strategy (in order):
 *
 * <ol>
 *   <li>The {@code MSAL_MTLS_HELPER_PATH} environment variable — an absolute path to a
 *       custom binary. Useful for testing against a locally-built helper.</li>
 *   <li>The bundled resource inside the JAR ({@code /MsalMtlsMsiHelper.exe}) — extracted to
 *       a temp file on first use and reused across subsequent calls within the same JVM.</li>
 * </ol>
 *
 * <p>This class is public so that integration tests can swap in a different locator
 * (e.g. one that always returns a mock binary path).</p>
 */
public class MtlsMsiHelperLocator {

    /** Environment variable override for the helper binary path. */
    public static final String ENV_HELPER_PATH = "MSAL_MTLS_HELPER_PATH";

    /** Bundled resource name inside the JAR. */
    private static final String BUNDLED_RESOURCE = "/MsalMtlsMsiHelper.exe";

    private static volatile Path extractedPath;
    private static final Object EXTRACT_LOCK = new Object();

    /**
     * Returns the absolute path to a usable {@code MsalMtlsMsiHelper.exe}.
     *
     * @return never null
     * @throws MtlsMsiException if the binary cannot be located or extracted
     */
    public String locate() throws MtlsMsiException {
        // 1. Environment variable override
        String envPath = System.getenv(ENV_HELPER_PATH);
        if (envPath != null && !envPath.isEmpty()) {
            File f = new File(envPath);
            if (!f.isFile()) {
                throw new MtlsMsiException(
                        "MSAL_MTLS_HELPER_PATH points to a non-existent file: " + envPath);
            }
            return f.getAbsolutePath();
        }

        // 2. Bundled resource in JAR → extract to temp once
        if (extractedPath != null && Files.isRegularFile(extractedPath)) {
            return extractedPath.toString();
        }

        synchronized (EXTRACT_LOCK) {
            if (extractedPath != null && Files.isRegularFile(extractedPath)) {
                return extractedPath.toString();
            }
            extractedPath = extractBundled();
        }
        return extractedPath.toString();
    }

    private Path extractBundled() throws MtlsMsiException {
        try (InputStream in = MtlsMsiHelperLocator.class.getResourceAsStream(BUNDLED_RESOURCE)) {
            if (in == null) {
                throw new MtlsMsiException(
                        "MsalMtlsMsiHelper.exe not found in the msal4j-mtls-extensions JAR. " +
                        "Ensure the JAR was built with the binary, or set MSAL_MTLS_HELPER_PATH " +
                        "to point to the binary manually.");
            }
            Path tmp = Files.createTempFile("MsalMtlsMsiHelper-", ".exe");
            tmp.toFile().deleteOnExit();
            Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            tmp.toFile().setExecutable(true);
            return tmp;
        } catch (IOException e) {
            throw new MtlsMsiException("Failed to extract MsalMtlsMsiHelper.exe from JAR: " + e.getMessage(), e);
        }
    }
}
