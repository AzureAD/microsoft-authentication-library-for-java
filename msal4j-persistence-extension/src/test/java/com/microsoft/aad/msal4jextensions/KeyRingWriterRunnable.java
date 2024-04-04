// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.linux.KeyRingAccessor;

public class KeyRingWriterRunnable extends CacheWriterRunnable {

    KeyRingWriterRunnable
            (String id,
             String lockFilePath, String filePath, String lockHoldingIntervalsFilePath,
             String schema,
             String label,
             String attribute1Key,
             String attribute1Value,
             String attribute2Key,
             String attribute2Value) {

        this.lockHoldingIntervalsFilePath = lockHoldingIntervalsFilePath;

        lock = new CrossProcessCacheFileLock(lockFilePath, 150, 100);

        cacheAccessor = new KeyRingAccessor(filePath,
                null,
                schema,
                label,
                attribute1Key,
                attribute1Value,
                attribute2Key,
                attribute2Value);
    }
}
