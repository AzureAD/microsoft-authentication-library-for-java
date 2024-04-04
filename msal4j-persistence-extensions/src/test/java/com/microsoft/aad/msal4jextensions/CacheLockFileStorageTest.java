// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4jextensions;

import com.microsoft.aad.msal4jextensions.persistence.CacheFileAccessor;
import org.junit.Test;

import java.io.IOException;

public class CacheLockFileStorageTest extends CacheLockTestBase {

    CacheFileAccessor cacheFileAccessor = new CacheFileAccessor(testFilePath);

    class CacheFileWriterRunnableFactory implements IRunnableFactory {
        @Override
        public Runnable create(String id) {
            return new CacheFileWriterRunnable
                    (id, lockFilePath, testFilePath, lockHoldingIntervalsFilePath);
        }
    }

    @Test
    public void multipleThreadsWriting_CacheFile() throws IOException, InterruptedException {
        int numOfThreads = 50;

        multipleThreadsWriting(cacheFileAccessor, numOfThreads, new CacheFileWriterRunnableFactory());
    }

    //@Test
    public void multipleProcessesWriting_CacheFile() throws IOException, InterruptedException {
        int numOfProcesses = 20;

        String writerClass = com.microsoft.aad.msal4jextensions.CacheFileWriter.class.getName();

        String writerClassArgs = lockFilePath + " " +
                testFilePath + " " +
                lockHoldingIntervalsFilePath;

        multipleProcessesWriting(cacheFileAccessor, numOfProcesses, writerClass, writerClassArgs);
    }
}
