// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4j.ITokenCacheAccessAspect;
import com.microsoft.aad.msal4j.ITokenCacheAccessContext;
import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;
import com.microsoft.aad.msal4jextensions.persistence.ICacheAccessor;
import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingAccessor;
import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;
import com.nimbusds.jose.util.StandardCharset;
import com.sun.jna.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Implementation of ITokenCacheAccessAspect which store MSAL token cache
 * in platform specific secret storage:
 * Win - file encrypted with DPAPI
 * Mac - key chain
 * Linux - key ring
 */
public class PersistenceTokenCacheAccessAspect implements ITokenCacheAccessAspect {
    private final static Logger LOG = LoggerFactory.getLogger(PersistenceTokenCacheAccessAspect.class);

    private CrossProcessCacheFileLock lock;
    private Long lastSeenCacheFileModifiedTimestamp;
    private ICacheAccessor cacheAccessor;

    private PersistenceSettings parameters;

    private String getCacheLockFilePath() {
        return parameters.getCacheDirectoryPath() + File.separator + ".lockfile";
    }

    private String getCacheFilePath() {
        return parameters.getCacheDirectoryPath() + File.separator + parameters.getCacheFileName();
    }

    private void createCacheFileIfNotExist() throws IOException {
        Files.createDirectories(parameters.getCacheDirectoryPath());

        if(new File(getCacheFilePath()).createNewFile()){
            LOG.debug("MSAL cache file was created, path - " + getCacheFilePath());
        }
    }

    public PersistenceTokenCacheAccessAspect(PersistenceSettings persistenceSettings) throws IOException {
        this.parameters = persistenceSettings;

        createCacheFileIfNotExist();

        String cacheFilePath = getCacheFilePath();

        lock = new CrossProcessCacheFileLock(getCacheLockFilePath(),
                persistenceSettings.getLockRetryDelayMilliseconds(),
                persistenceSettings.getLockRetryNumber());

        if (Platform.isMac()) {
            cacheAccessor = new KeyChainAccessor(
                    cacheFilePath, parameters.getKeychainService(), parameters.getKeychainAccount());

        } else if (Platform.isWindows()) {
            cacheAccessor = new CacheFileAccessor(cacheFilePath);

        } else if (Platform.isLinux()) {
            if (parameters.isOnLinuxUseUnprotectedFileAsCacheStorage()) {
                cacheAccessor = new CacheFileAccessor(cacheFilePath);
            } else {
                cacheAccessor = new KeyRingAccessor(cacheFilePath,
                        parameters.getKeyringCollection(),
                        parameters.getKeyringSchemaName(),
                        parameters.getKeyringSecretLabel(),
                        parameters.getKeyringAttribute1Key(),
                        parameters.getKeyringAttribute1Value(),
                        parameters.getKeyringAttribute2Key(),
                        parameters.getKeyringAttribute2Value());

                try{
                    lock.lock();
                    ((KeyRingAccessor) cacheAccessor).verify();
                }
                finally {
                    lock.unlock();
                }
            }
        }
    }

    private boolean isWriteAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        return iTokenCacheAccessContext.hasCacheChanged();
    }

    private boolean isReadAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        return !isWriteAccess(iTokenCacheAccessContext);
    }

    private void updateLastSeenCacheFileModifiedTimestamp() {
        lastSeenCacheFileModifiedTimestamp = getCurrentCacheFileModifiedTimestamp();
    }

    public Long getCurrentCacheFileModifiedTimestamp() {
        return new File(getCacheFilePath()).lastModified();
    }

    @Override
    public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        try {
            if (isWriteAccess(iTokenCacheAccessContext)) {
                lock.lock();
            } else {
                Long currentCacheFileModifiedTimestamp = getCurrentCacheFileModifiedTimestamp();
                if (currentCacheFileModifiedTimestamp != null &&
                        currentCacheFileModifiedTimestamp.equals(lastSeenCacheFileModifiedTimestamp)) {
                    return;
                } else {
                    lock.lock();
                }
            }
            byte[] data = cacheAccessor.read();
            if (data != null) {
                iTokenCacheAccessContext.tokenCache().deserialize(new String(data, StandardCharset.UTF_8));
            }

            updateLastSeenCacheFileModifiedTimestamp();

            if (isReadAccess(iTokenCacheAccessContext)) {
                lock.unlock();
            }
        } catch (IOException ex) {
            LOG.error(ex.getMessage());
        }
    }

    @Override
    public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
        try {
            if (isWriteAccess(iTokenCacheAccessContext)) {
                cacheAccessor.write(iTokenCacheAccessContext.tokenCache().serialize().getBytes(StandardCharset.UTF_8));
                updateLastSeenCacheFileModifiedTimestamp();
            }
        } finally {
            if(lock != null) {
                try {
                    lock.unlock();
                } catch (IOException e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }
}
