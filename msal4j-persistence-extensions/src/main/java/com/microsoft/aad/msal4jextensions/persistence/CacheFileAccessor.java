// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions.persistence;

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Crypt32Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;

/**
 * Implementation of CacheAccessor based on File persistence
 */
public class CacheFileAccessor implements ICacheAccessor {
    private final static Logger LOG = LoggerFactory.getLogger(CacheFileAccessor.class);

    private String cacheFilePath;
    private File cacheFile;

    public CacheFileAccessor(String cacheFilePath) {
        this.cacheFilePath = cacheFilePath;

        cacheFile = new File(cacheFilePath);
    }

    @Override
    public byte[] read() {
        byte[] data = null;

        if (cacheFile.exists()) {
            try {
                data = Files.readAllBytes(cacheFile.toPath());
            } catch (IOException e) {
                throw new CacheFileAccessException("Failed to read Cache File", e);
            }

            if (data != null && data.length > 0 && Platform.isWindows()) {
                data = Crypt32Util.cryptUnprotectData(data);
            }
        }

        return data;
    }

    @Override
    public void write(byte[] data) {
        if (Platform.isWindows()) {
            data = Crypt32Util.cryptProtectData(data);

            try (FileOutputStream stream = new FileOutputStream(cacheFile)) {
                stream.write(data);
            }
            catch (IOException e) {
                throw new CacheFileAccessException("Failed to write to Cache File", e);
            }
        }
        else {
            writeAtomic(data);
        }
    }

    private void writeAtomic(byte[] data) {
        File tempFile = null;
        try {
            try {
                tempFile = File.createTempFile("JavaMsalExtTemp", ".tmp", cacheFile.getParentFile());

                try (FileOutputStream stream = new FileOutputStream(tempFile)) {
                    stream.write(data);
                }

                Files.move(tempFile.toPath(), cacheFile.toPath(),
                        StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } finally {
                if (tempFile != null) {
                    Files.deleteIfExists(tempFile.toPath());
                }
            }
        }
        catch (IOException e) {
            throw new CacheFileAccessException("Failed to write to Cache File", e);
        }
    }

    @Override
    public void delete() {
        try {
            Files.deleteIfExists(new File(cacheFilePath).toPath());
        } catch (IOException e) {
            throw new CacheFileAccessException("Failed to delete Cache File", e);
        }
    }

    public void updateCacheFileLastModifiedTime() {
        FileTime fileTime = FileTime.fromMillis(System.currentTimeMillis());

        try {
            Files.setLastModifiedTime(Paths.get(cacheFilePath), fileTime);
        } catch (IOException e) {
            throw new CacheFileAccessException("Failed to set lastModified time on Cache File", e);
        }
    }
}
