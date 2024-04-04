// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.mac.KeyChainAccessor;

public class KeyChainWriterRunnable extends CacheWriterRunnable {

    KeyChainWriterRunnable
            (String id, String lockFilePath, String filePath, String lockHoldingIntervalsFilePath,
             String serviceName, String accountName) {

        this.lockHoldingIntervalsFilePath = lockHoldingIntervalsFilePath;

        lock = new CrossProcessCacheFileLock(lockFilePath, 150, 100);

        cacheAccessor = new KeyChainAccessor(filePath, serviceName, accountName);
    }
}
