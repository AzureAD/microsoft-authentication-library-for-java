// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;

class CacheFileWriterRunnable extends CacheWriterRunnable {

    CacheFileWriterRunnable(String id, String lockFilePath, String filePath, String lockHoldingIntervalsFilePath) {
        this.lockHoldingIntervalsFilePath = lockHoldingIntervalsFilePath;

        lock = new CrossProcessCacheFileLock(lockFilePath, 150, 100);
        cacheAccessor = new CacheFileAccessor(filePath);
    }
}